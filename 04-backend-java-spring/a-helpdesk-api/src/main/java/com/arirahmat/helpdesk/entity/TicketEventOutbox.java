package com.arirahmat.helpdesk.entity;

import com.arirahmat.helpdesk.event.TicketEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ticket_event_outbox", indexes =
        @Index(name = "idx_ticket_event_outbox_pending", columnList = "published_at, occurred_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketEventOutbox {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private Long ticketId;

    @Column(name = "owner_email", nullable = false, length = 120, updatable = false)
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TicketStatus status;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    private TicketEventOutbox(TicketEvent event) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.eventVersion = event.eventVersion();
        this.ticketId = event.ticketId();
        this.ownerEmail = event.ownerEmail();
        this.status = event.status();
        this.occurredAt = event.occurredAt();
    }

    public static TicketEventOutbox from(TicketEvent event) {
        return new TicketEventOutbox(Objects.requireNonNull(event, "event"));
    }

    public TicketEvent toEvent() {
        return new TicketEvent(eventId, eventType, eventVersion, ticketId,
                ownerEmail, status, occurredAt);
    }

    public void markPublished(Instant acknowledgedAt) {
        this.publishedAt = Objects.requireNonNull(acknowledgedAt, "acknowledgedAt");
    }
}
