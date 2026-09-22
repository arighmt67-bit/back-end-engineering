package com.arirahmat.helpdesk.dto.report;

import com.arirahmat.helpdesk.entity.TicketStatus;

public record StatusCountDto(TicketStatus status, long total) {
}
