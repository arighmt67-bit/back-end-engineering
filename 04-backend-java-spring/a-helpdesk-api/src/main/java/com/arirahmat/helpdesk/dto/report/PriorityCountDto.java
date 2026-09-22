package com.arirahmat.helpdesk.dto.report;

import com.arirahmat.helpdesk.entity.TicketPriority;

public record PriorityCountDto(TicketPriority priority, long total) {
}
