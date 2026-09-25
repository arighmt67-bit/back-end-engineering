package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.exception.ResourceNotFoundException;
import com.arirahmat.helpdesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketReadServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketReadService ticketReadService;
    private ConcurrentMapCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("ticketById");
        ticketReadService = new TicketReadService(
                ticketRepository, cacheManager, new ResilientCacheErrorHandler());
    }

    @Test
    @DisplayName("Loader detail tiket membaca PostgreSQL dan memetakan response")
    void membaca_detail_tiket_dari_repository() {
        User owner = User.builder().id(1L).email("owner@example.com").fullName("Owner")
                .passwordHash("hash").role(Role.ROLE_USER).build();
        Ticket ticket = Ticket.builder().id(10L).title("Printer error")
                .description("Tidak bisa mencetak").status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH).owner(owner)
                .updatedAt(Instant.parse("2026-09-24T13:00:00Z")).build();
        when(ticketRepository.findUpdatedAtById(10L))
                .thenReturn(Optional.of(ticket.getUpdatedAt()));
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketReadService.getById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.ownerEmail()).isEqualTo("owner@example.com");
        assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    @DisplayName("Loader detail tiket menolak ID yang tidak ada")
    void tiket_tidak_ada_melempar_resource_not_found() {
        when(ticketRepository.findUpdatedAtById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketReadService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Cache stale tidak dipakai ketika updatedAt database sudah berubah")
    void cache_stale_diganti_dengan_snapshot_database_terbaru() {
        Instant oldRevision = Instant.parse("2026-09-24T13:00:00Z");
        Instant newRevision = Instant.parse("2026-09-24T13:05:00Z");
        Cache cache = cacheManager.getCache("ticketById");
        assertThat(cache).isNotNull();
        cache.put(10L, new TicketResponse(10L, "Judul lama", "Deskripsi lama",
                TicketStatus.OPEN, TicketPriority.MEDIUM, "owner@example.com",
                oldRevision, oldRevision, null));

        User owner = User.builder().id(1L).email("owner@example.com").fullName("Owner")
                .passwordHash("hash").role(Role.ROLE_USER).build();
        Ticket current = Ticket.builder().id(10L).title("Judul baru")
                .description("Deskripsi baru").status(TicketStatus.IN_PROGRESS)
                .priority(TicketPriority.HIGH).owner(owner)
                .createdAt(oldRevision).updatedAt(newRevision).build();
        when(ticketRepository.findUpdatedAtById(10L)).thenReturn(Optional.of(newRevision));
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(current));

        TicketResponse response = ticketReadService.getById(10L);

        assertThat(response.title()).isEqualTo("Judul baru");
        assertThat(cache.get(10L, TicketResponse.class)).isEqualTo(response);
    }
}
