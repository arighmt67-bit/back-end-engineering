package com.arirahmat.helpdesk.controller;

import com.arirahmat.helpdesk.dto.report.AgentWorkloadDto;
import com.arirahmat.helpdesk.dto.report.MonthlyCountDto;
import com.arirahmat.helpdesk.dto.report.PriorityCountDto;
import com.arirahmat.helpdesk.dto.report.StatusCountDto;
import com.arirahmat.helpdesk.dto.report.SummaryReport;
import com.arirahmat.helpdesk.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

/**
 * Endpoint agregasi untuk dashboard. Dibatasi role AGENT/ADMIN lewat SecurityConfig.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "3. Reports", description = "Agregasi data tiket untuk dashboard (khusus AGENT/ADMIN)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Ringkasan menyeluruh",
               description = "Total tiket, tiket terbuka, tiket selesai, rata-rata jam penyelesaian, "
                       + "serta breakdown per status dan per prioritas.")
    public ResponseEntity<SummaryReport> summary() {
        return ResponseEntity.ok(reportService.summary());
    }

    @GetMapping("/monthly")
    @Operation(summary = "Jumlah tiket per bulan dalam satu tahun")
    public ResponseEntity<List<MonthlyCountDto>> monthly(
            @Parameter(description = "Tahun, default tahun berjalan")
            @RequestParam(required = false) Integer year) {
        int target = (year == null) ? Year.now().getValue() : year;
        return ResponseEntity.ok(reportService.monthly(target));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Jumlah tiket dikelompokkan per status")
    public ResponseEntity<List<StatusCountDto>> byStatus() {
        return ResponseEntity.ok(reportService.byStatus());
    }

    @GetMapping("/by-priority")
    @Operation(summary = "Jumlah tiket dikelompokkan per prioritas")
    public ResponseEntity<List<PriorityCountDto>> byPriority() {
        return ResponseEntity.ok(reportService.byPriority());
    }

    @GetMapping("/workload")
    @Operation(summary = "Beban tiket per user",
               description = "Total tiket dan tiket yang masih OPEN untuk tiap pemilik tiket.")
    public ResponseEntity<List<AgentWorkloadDto>> workload() {
        return ResponseEntity.ok(reportService.workload());
    }
}
