package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketCacheBehaviorTest {

    @Test
    @DisplayName("Cache hit hanya menjalankan revision probe tanpa memuat snapshot penuh lagi")
    void cache_hit_hanya_memuat_snapshot_database_sekali() {
        TicketRepository repository = mock(TicketRepository.class);
        Ticket ticket = ticket(10L);
        when(repository.findUpdatedAtById(10L)).thenReturn(Optional.of(ticket.getUpdatedAt()));
        when(repository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));
        CacheManager cacheManager = new ConcurrentMapCacheManager("ticketById");

        try (AnnotationConfigApplicationContext context = context(repository, cacheManager)) {
            TicketReadService service = context.getBean(TicketReadService.class);

            TicketResponse first = service.getById(10L);
            TicketResponse second = service.getById(10L);

            assertThat(second).isEqualTo(first);
            verify(repository, times(1)).findByIdWithOwner(10L);
            verify(repository, times(2)).findUpdatedAtById(10L);
        }
    }

    @Test
    @DisplayName("Redis gagal saat get dan put tetap fallback ke PostgreSQL")
    void cache_error_tetap_mengembalikan_data_repository() {
        TicketRepository repository = mock(TicketRepository.class);
        Ticket ticket = ticket(10L);
        when(repository.findUpdatedAtById(10L)).thenReturn(Optional.of(ticket.getUpdatedAt()));
        when(repository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));
        Cache brokenCache = mock(Cache.class);
        when(brokenCache.getName()).thenReturn("ticketById");
        when(brokenCache.get(10L, TicketResponse.class))
                .thenThrow(new IllegalStateException("Redis tidak tersedia"));
        doThrow(new IllegalStateException("Redis tidak tersedia"))
                .when(brokenCache).put(org.mockito.ArgumentMatchers.eq(10L), any());
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("ticketById")).thenReturn(brokenCache);

        try (AnnotationConfigApplicationContext context = context(repository, cacheManager)) {
            TicketResponse response = context.getBean(TicketReadService.class).getById(10L);

            assertThat(response.id()).isEqualTo(10L);
            verify(repository).findByIdWithOwner(10L);
        }
    }

    private AnnotationConfigApplicationContext context(
            TicketRepository repository, CacheManager cacheManager) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(CacheManager.class, () -> cacheManager);
        ResilientCacheErrorHandler errorHandler = new ResilientCacheErrorHandler();
        context.registerBean(ResilientCacheErrorHandler.class, () -> errorHandler);
        context.register(CacheConfig.class);
        context.registerBean(TicketReadService.class,
                () -> new TicketReadService(repository, cacheManager, errorHandler));
        context.refresh();
        return context;
    }

    private Ticket ticket(Long id) {
        User owner = User.builder().id(1L).email("owner@example.com").fullName("Owner")
                .passwordHash("hash").role(Role.ROLE_USER).build();
        return Ticket.builder().id(id).title("Printer error").description("Tidak bisa mencetak")
                .status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).owner(owner)
                .createdAt(Instant.parse("2026-09-24T13:00:00Z"))
                .updatedAt(Instant.parse("2026-09-24T13:00:00Z")).build();
    }
}
