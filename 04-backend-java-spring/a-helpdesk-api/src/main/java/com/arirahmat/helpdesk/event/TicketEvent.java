package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record TicketEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Long ticketId,
        String ownerEmail,
        TicketStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt
) {
    public static final String CREATED = "ticket.created.v1";
    public static final String STATUS_CHANGED = "ticket.status-changed.v1";
    public static final int VERSION = 1;

    public static TicketEvent created(Ticket ticket) {
        return from(ticket, CREATED);
    }

    public static TicketEvent statusChanged(Ticket ticket) {
        return from(ticket, STATUS_CHANGED);
    }

    private static TicketEvent from(Ticket ticket, String eventType) {
        return new TicketEvent(
                UUID.randomUUID(),
                eventType,
                VERSION,
                ticket.getId(),
                ticket.getOwner().getEmail(),
                ticket.getStatus(),
                Instant.now());
    }
}
