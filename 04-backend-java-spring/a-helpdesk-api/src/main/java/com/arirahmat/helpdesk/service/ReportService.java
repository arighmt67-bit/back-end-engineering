package com.arirahmat.helpdesk.service;

import com.arirahmat.helpdesk.dto.report.AgentWorkloadDto;
import com.arirahmat.helpdesk.dto.report.MonthlyCountDto;
import com.arirahmat.helpdesk.dto.report.PriorityCountDto;
import com.arirahmat.helpdesk.dto.report.StatusCountDto;
import com.arirahmat.helpdesk.dto.report.SummaryReport;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Semua perhitungan report dilakukan lewat query agregasi di sisi database
 * (GROUP BY / COUNT), bukan dengan menarik seluruh baris ke memori.
 */
@Service
public class ReportService {

    private final TicketRepository ticketRepository;

    public ReportService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public SummaryReport summary() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED)
                + ticketRepository.countByStatus(TicketStatus.CLOSED);

        return new SummaryReport(
                total, open, resolved, averageResolutionHours(),
                ticketRepository.countGroupByStatus(),
                ticketRepository.countGroupByPriority());
    }

    @Transactional(readOnly = true)
    public List<MonthlyCountDto> monthly(int year) {
        return ticketRepository.countMonthlyByYear(year);
    }

    @Transactional(readOnly = true)
    public List<AgentWorkloadDto> workload() {
        return ticketRepository.findWorkloadPerUser();
    }

    @Transactional(readOnly = true)
    public List<StatusCountDto> byStatus() {
        return ticketRepository.countGroupByStatus();
    }

    @Transactional(readOnly = true)
    public List<PriorityCountDto> byPriority() {
        return ticketRepository.countGroupByPriority();
    }

    /** Null bila belum ada tiket yang selesai, supaya tidak menampilkan 0.0 yang menyesatkan. */
    private Double averageResolutionHours() {
        List<Object[]> rows = ticketRepository.findResolutionTimestamps();
        if (rows.isEmpty()) {
            return null;
        }
        double totalHours = 0;
        for (Object[] row : rows) {
            Instant created = (Instant) row[0];
            Instant resolved = (Instant) row[1];
            totalHours += Duration.between(created, resolved).toMinutes() / 60.0;
        }
        return Math.round((totalHours / rows.size()) * 100.0) / 100.0;
    }
}
