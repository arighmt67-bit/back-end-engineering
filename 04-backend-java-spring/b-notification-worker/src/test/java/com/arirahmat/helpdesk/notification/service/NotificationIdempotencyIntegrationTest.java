package com.arirahmat.helpdesk.notification.service;

import com.arirahmat.helpdesk.notification.entity.NotificationLog;
import com.arirahmat.helpdesk.notification.event.TicketEvent;
import com.arirahmat.helpdesk.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:worker-idempotency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class NotificationIdempotencyIntegrationTest {

    @Autowired private NotificationService service;
    @Autowired private NotificationLogRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void same_event_consumed_twice_creates_one_notification() {
        TicketEvent event = event();

        service.process(event);
        service.process(event);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().get(0).getEventId()).isEqualTo(event.eventId());
    }

    @Test
    void database_unique_constraint_is_final_idempotency_guard() {
        TicketEvent event = event();
        repository.saveAndFlush(log(event));

        assertThatThrownBy(() -> repository.saveAndFlush(log(event)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private TicketEvent event() {
        return new TicketEvent(UUID.randomUUID(), TicketEvent.CREATED, TicketEvent.VERSION,
                42L, "owner@example.com", "OPEN",
                Instant.parse("2026-09-24T13:00:00Z"));
    }

    private NotificationLog log(TicketEvent event) {
        return new NotificationLog(event.eventId(), event.eventType(), event.ticketId(),
                event.ownerEmail(), "Ticket #42 status OPEN", event.occurredAt(), Instant.now());
    }
}
