package com.arirahmat.helpdesk.dto;

import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String ownerEmail,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getStatus(), t.getPriority(), t.getOwner().getEmail(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt());
    }
}
