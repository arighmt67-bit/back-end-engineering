package com.arirahmat.helpdesk.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_log", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_log_event_id", columnNames = "event_id"))
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    private String eventType;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private Long ticketId;

    @Column(nullable = false, length = 320, updatable = false)
    private String recipient;

    @Column(nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected NotificationLog() {
    }

    public NotificationLog(UUID eventId, String eventType, Long ticketId, String recipient,
                           String message, Instant occurredAt, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.ticketId = ticketId;
        this.recipient = recipient;
        this.message = message;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Long getTicketId() { return ticketId; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getProcessedAt() { return processedAt; }
}
