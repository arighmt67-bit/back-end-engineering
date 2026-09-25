package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketRequest;
import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.repository.TicketEventOutboxRepository;
import com.arirahmat.helpdesk.repository.TicketRepository;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.arirahmat.helpdesk.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketCacheInvalidationTest {

    @Test
    @DisplayName("Update sukses hanya menghapus cache ID tiket yang berubah")
    void update_menghapus_cache_id_yang_tepat() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        try (Fixture fixture = fixture(owner, ticket(10L, owner))) {
            fixture.service().update(10L,
                    new TicketRequest("Judul baru", "Deskripsi baru", TicketPriority.HIGH),
                    owner.getEmail());

            assertEvictedOnlyTarget(fixture.cache());
        }
    }

    @Test
    @DisplayName("Perubahan status sukses hanya menghapus cache ID tiket yang berubah")
    void change_status_menghapus_cache_id_yang_tepat() {
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        User owner = user("owner@example.com", Role.ROLE_USER);
        try (Fixture fixture = fixture(agent, ticket(10L, owner))) {
            fixture.service().changeStatus(10L, TicketStatus.RESOLVED, agent.getEmail());

            assertEvictedOnlyTarget(fixture.cache());
        }
    }

    @Test
    @DisplayName("Delete sukses hanya menghapus cache ID tiket yang berubah")
    void delete_menghapus_cache_id_yang_tepat() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        try (Fixture fixture = fixture(owner, ticket(10L, owner))) {
            fixture.service().delete(10L, owner.getEmail());

            assertEvictedOnlyTarget(fixture.cache());
        }
    }

    private Fixture fixture(User actor, Ticket ticket) {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("ticketById");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(CacheManager.class, () -> cacheManager);
        context.registerBean(ResilientCacheErrorHandler.class);
        context.register(CacheConfig.class);
        context.registerBean(TicketRepository.class, () -> ticketRepository);
        context.registerBean(UserRepository.class, () -> userRepository);
        context.registerBean(TicketEventOutboxRepository.class,
                () -> mock(TicketEventOutboxRepository.class));
        context.registerBean(TicketReadService.class, () -> mock(TicketReadService.class));
        context.registerBean(TicketService.class);
        context.refresh();

        Cache cache = cacheManager.getCache("ticketById");
        if (cache == null) {
            throw new IllegalStateException("Cache ticketById tidak tersedia");
        }
        cache.put(10L, response(10L));
        cache.put(20L, response(20L));
        return new Fixture(context, context.getBean(TicketService.class), cache);
    }

    private void assertEvictedOnlyTarget(Cache cache) {
        assertThat(cache.get(10L)).isNull();
        assertThat(cache.get(20L)).isNotNull();
    }

    private User user(String email, Role role) {
        return User.builder().id(1L).email(email).fullName("Nama")
                .passwordHash("hash").role(role).build();
    }

    private Ticket ticket(Long id, User owner) {
        return Ticket.builder().id(id).title("Printer error").description("Tidak bisa mencetak")
                .status(TicketStatus.OPEN).priority(TicketPriority.MEDIUM).owner(owner).build();
    }

    private TicketResponse response(Long id) {
        return new TicketResponse(id, "Judul", "Deskripsi", TicketStatus.OPEN,
                TicketPriority.MEDIUM, "owner@example.com", null, null, null);
    }

    private record Fixture(
            AnnotationConfigApplicationContext context,
            TicketService service,
            Cache cache
    ) implements AutoCloseable {
        @Override
        public void close() {
            context.close();
        }
    }
}
