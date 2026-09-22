package com.arirahmat.helpdesk.dto.report;

import java.util.List;

/**
 * Agregasi tingkat tinggi untuk dashboard helpdesk.
 * avgResolutionHours dihitung hanya dari tiket yang sudah RESOLVED/CLOSED.
 */
public record SummaryReport(
        long totalTickets,
        long openTickets,
        long resolvedTickets,
        Double avgResolutionHours,
        List<StatusCountDto> byStatus,
        List<PriorityCountDto> byPriority
) {
}
