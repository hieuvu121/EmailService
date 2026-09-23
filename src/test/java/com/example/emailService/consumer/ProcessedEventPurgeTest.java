package com.example.emailService.consumer;

import com.example.emailService.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessedEventPurgeTest {

    @Mock private ProcessedEventRepository processedEventRepository;

    @InjectMocks private ProcessedEventPurge purge;

    @Test
    void theCutoffIsTheConfiguredNumberOfDaysBack() {
        ReflectionTestUtils.setField(purge, "retentionDays", 14);
        Instant before = Instant.now();

        purge.purge();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(processedEventRepository).deleteProcessedBefore(cutoff.capture());

        /*
         * Asserts the window rather than an exact instant: the cutoff is
         * computed from Instant.now() inside the method, so pinning a value
         * would only test the clock.
         */
        assertThat(cutoff.getValue())
                .isBefore(before.minus(13, ChronoUnit.DAYS))
                .isAfter(before.minus(15, ChronoUnit.DAYS));
    }

    /** A shorter window must actually shorten it — the property has to be read. */
    @Test
    void aShorterRetentionMovesTheCutoffCloser() {
        ReflectionTestUtils.setField(purge, "retentionDays", 1);
        Instant before = Instant.now();

        purge.purge();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(processedEventRepository).deleteProcessedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isAfter(before.minus(2, ChronoUnit.DAYS));
    }
}
