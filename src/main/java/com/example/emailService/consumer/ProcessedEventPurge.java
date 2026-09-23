package com.example.emailService.consumer;

import com.example.emailService.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Keeps processed_event from growing without bound.
 *
 * The retention window only has to outlive redelivery, which is bounded by
 * Kafka's own retention and four retry attempts -- days, not months. A row
 * older than that can never be matched again, so keeping it buys nothing and
 * the table would otherwise grow one row per email forever.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventPurge {

    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.dedup.retention-days:14}")
    private int retentionDays;

    @Scheduled(cron = "${app.dedup.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purge() {
        int deleted = processedEventRepository.deleteProcessedBefore(
                Instant.now().minus(Duration.ofDays(retentionDays)));
        if (deleted > 0) {
            log.info("Purged {} processed-event rows", deleted);
        }
    }
}
