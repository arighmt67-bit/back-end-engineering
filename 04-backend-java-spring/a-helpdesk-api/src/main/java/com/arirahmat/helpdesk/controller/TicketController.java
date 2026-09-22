package com.arirahmat.helpdesk.controller;

import com.arirahmat.helpdesk.dto.PagedResponse;
import com.arirahmat.helpdesk.dto.TicketRequest;
import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.dto.TicketStatusRequest;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "2. Tickets", description = "CRUD tiket helpdesk. User biasa hanya melihat tiket miliknya sendiri.")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Buat tiket baru")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request,
                                                 @AuthenticationPrincipal String email) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request, email));
    }

    @GetMapping
    @Operation(summary = "Daftar tiket (paginated)",
               description = "AGENT/ADMIN melihat semua tiket; USER hanya tiket miliknya.")
    public ResponseEntity<PagedResponse<TicketResponse>> list(
            @Parameter(description = "Filter status, kosongkan untuk semua")
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @AuthenticationPrincipal String email) {

        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sortBy).descending());
        Page<TicketResponse> result = ticketService.list(status, email, pageable);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail satu tiket")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id,
                                                  @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ticketService.getById(id, email));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Perbarui judul, deskripsi, atau prioritas tiket")
    public ResponseEntity<TicketResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody TicketRequest request,
                                                 @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ticketService.update(id, request, email));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ubah status tiket (khusus AGENT/ADMIN)",
               description = "Status RESOLVED atau CLOSED otomatis mengisi resolvedAt.")
    public ResponseEntity<TicketResponse> changeStatus(@PathVariable Long id,
                                                       @Valid @RequestBody TicketStatusRequest request,
                                                       @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ticketService.changeStatus(id, request.status(), email));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus tiket")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal String email) {
        ticketService.delete(id, email);
        return ResponseEntity.noContent().build();
    }
}
