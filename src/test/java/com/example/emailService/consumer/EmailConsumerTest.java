package com.example.emailService.consumer;

import com.example.emailService.entity.ProcessedEvent;
import com.example.emailService.event.EmailEvent;
import com.example.emailService.repository.ProcessedEventRepository;
import com.example.emailService.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * email-service is the only consumer in the system with no natural key to be
 * idempotent on.
 *
 * Everywhere else there is a row that means "already done" -- settlement-service
 * checks existsByExpenseIdAndFromMemberId, the projection consumers upsert by
 * primary key. Sending an email is not repeatable and leaves no such trace, so
 * the producer's eventId is the key and processed_event is the memory.
 */
@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    private static final String EVENT_ID = "0b6a1f2c-1111-2222-3333-444455556666";

    @Mock private EmailService emailService;
    @Mock private ProcessedEventRepository processedEventRepository;

    @InjectMocks private EmailConsumer consumer;

    private static EmailEvent event(String eventId) {
        EmailEvent e = new EmailEvent();
        e.setEventId(eventId);
        e.setTo("dana@example.com");
        e.setSubject("Activate your Expensphie account");
        e.setBody("Click here");
        e.setEventType("ACTIVATION");
        return e;
    }

    @Test
    void aFirstTimeEventIsSentAndRemembered() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);

        consumer.consume(event(EVENT_ID));

        verify(emailService).sendEmail("dana@example.com",
                "Activate your Expensphie account", "Click here");

        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getValue().getProcessedAt()).isNotNull();
    }

    /*
     * The case the whole change exists for. @RetryableTopic(attempts = "4")
     * republishes on failure and the outbox republishes anything it could not
     * confirm, so a redelivery is routine rather than exceptional.
     */
    @Test
    void anEventAlreadyProcessedIsNotSentAgain() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        consumer.consume(event(EVENT_ID));

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
    }

    /*
     * Anything published before this change, or by a producer that was missed.
     * Send it: an unidentifiable mail is worth delivering twice, never zero
     * times -- an activation email that never arrives is an account nobody can
     * activate.
     */
    @Test
    void anEventWithNoIdIsSentWithoutDedup() {
        consumer.consume(event(null));

        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
        verify(processedEventRepository, never()).existsById(anyString());
    }

    @Test
    void anEventWithABlankIdIsTreatedTheSameAsOneWithNone() {
        consumer.consume(event("   "));

        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
    }

    /*
     * Record after sending, never before. A crash in this window re-sends on
     * redelivery, which is the trade being made: recording first would lose the
     * mail entirely in the same window. The exception has to escape so the
     * retry topic sees the failure.
     */
    @Test
    void aFailedSendRemembersNothingSoTheRetryCanRunAgain() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        doThrow(new RuntimeException("SMTP refused"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> consumer.consume(event(EVENT_ID)))
                .isInstanceOf(RuntimeException.class);

        verify(processedEventRepository, never()).save(any());
    }
}
