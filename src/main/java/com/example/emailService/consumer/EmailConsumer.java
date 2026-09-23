package com.example.emailService.consumer;

import com.example.emailService.entity.ProcessedEvent;
import com.example.emailService.event.EmailEvent;
import com.example.emailService.repository.ProcessedEventRepository;
import com.example.emailService.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
//listen event-> convert data-> call service
@RetryableTopic(attempts = "4")
public class EmailConsumer {

    private final EmailService emailService;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Sends an email at most once per eventId.
     *
     * Every other consumer in the system is idempotent through a business key.
     * This one has nothing equivalent: sending is not repeatable and there is
     * no row that means "already sent". Redelivery here is routine rather than
     * exceptional -- @RetryableTopic republishes on failure, and auth-service's
     * outbox republishes anything it could not confirm -- so without this the
     * same activation mail goes out twice.
     *
     * DEDUP, NOT A GUARANTEE. A crash between the send and the save re-sends on
     * redelivery. That ordering is deliberate: recording first would mean a
     * crash in the same window loses the mail entirely, and an account whose
     * activation email never arrives cannot be activated at all. A duplicate is
     * an annoyance; a missing one is a dead account.
     */
    @KafkaListener(topics = "${kafka.topic.email}", groupId = "email-id")
    public void consume(EmailEvent emailEvent) {
        String eventId = emailEvent.getEventId();

        /*
         * Published before this field existed, or by a producer that was
         * missed. Send it: an unidentifiable mail is worth delivering twice,
         * never zero times. The warning is how you notice the missed producer.
         */
        if (eventId == null || eventId.isBlank()) {
            log.warn("EmailEvent for {} carries no eventId; sending without dedup", emailEvent.getTo());
            emailService.sendEmail(emailEvent.getTo(), emailEvent.getSubject(), emailEvent.getBody());
            return;
        }

        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping duplicate EmailEvent eventId={}, to={}", eventId, emailEvent.getTo());
            return;
        }

        emailService.sendEmail(emailEvent.getTo(), emailEvent.getSubject(), emailEvent.getBody());
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }

    /*
     * Was missing @DltHandler entirely, and logged through a `private Logger
     * log` field that nothing ever assigned -- so retry exhaustion was
     * invisible, and would have thrown NullPointerException had it ever been
     * reached. @Slf4j on the class supplies the logger now.
     */
    @DltHandler
    public void listenDLT(EmailEvent emailEvent) {
        log.error("email-events exhausted retries -- moved to DLT: to={}, eventType={}, eventId={}",
                emailEvent.getTo(), emailEvent.getEventType(), emailEvent.getEventId());
    }
}
