package com.example.emailService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per email already sent.
 *
 * email-service is the only consumer in the system with no natural key to be
 * idempotent on. Everywhere else there is a row that already means "done" --
 * settlement-service checks existsByExpenseIdAndFromMemberId, the projection
 * consumers upsert by primary key. Sending an email is not repeatable and
 * leaves no such trace, so the producer's eventId is the key and this table is
 * the memory.
 *
 * First entity in email_db, which until now was configured and empty.
 */
@Entity
@Table(name = "processed_event", indexes = {
        @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
