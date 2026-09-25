package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.dto.TicketRequest;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.repository.TicketEventOutboxRepository;
import com.arirahmat.helpdesk.repository.TicketRepository;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.arirahmat.helpdesk.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "spring.flyway.enabled=false",
        "app.kafka.enabled=false"
})
class TicketOutboxTransactionIT {

    @Autowired private TicketService ticketService;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private TicketEventOutboxRepository outboxRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String ownerEmail = "outbox-owner@example.com";

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .email(ownerEmail)
                .passwordHash("hash")
                .fullName("Outbox Owner")
                .role(Role.ROLE_USER)
                .build());
    }

    @Test
    void commit_menyimpan_ticket_dan_event_outbox_bersamaan() {
        ticketService.create(request(), ownerEmail);

        assertThat(ticketRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.findAll().get(0).toEvent().eventType())
                .isEqualTo(TicketEvent.CREATED);
    }

    @Test
    void rollback_membatalkan_ticket_dan_event_outbox_bersamaan() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            ticketService.create(request(), ownerEmail);
            assertThat(ticketRepository.count()).isEqualTo(1);
            assertThat(outboxRepository.count()).isEqualTo(1);
            status.setRollbackOnly();
        });

        assertThat(ticketRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    private TicketRequest request() {
        return new TicketRequest("Printer error", "Tidak bisa mencetak", TicketPriority.HIGH);
    }
}
