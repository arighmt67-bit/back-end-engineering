package com.arirahmat.helpdesk.dto;

import com.arirahmat.helpdesk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusRequest(@NotNull TicketStatus status) {
}
