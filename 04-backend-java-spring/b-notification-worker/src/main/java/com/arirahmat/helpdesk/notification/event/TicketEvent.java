package com.arirahmat.helpdesk.notification.event;

import java.time.Instant;
import java.util.UUID;

public record TicketEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Long ticketId,
        String ownerEmail,
        String status,
        Instant occurredAt
) {
    public static final String CREATED = "ticket.created.v1";
    public static final String STATUS_CHANGED = "ticket.status-changed.v1";
    public static final int VERSION = 1;
}
