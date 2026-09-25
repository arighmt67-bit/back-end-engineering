package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.entity.TicketEventOutbox;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.repository.TicketEventOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketEventOutboxRelayTest {

    @Mock
    private TicketEventOutboxRepository repository;

    @Mock
    private KafkaTicketEventPublisher publisher;

    private TicketEvent event;
    private TicketEventOutbox outbox;
    private TicketEventOutboxRelay relay;

    @BeforeEach
    void setUp() {
        event = new TicketEvent(UUID.randomUUID(), TicketEvent.CREATED, TicketEvent.VERSION, 42L,
                "owner@example.com", TicketStatus.OPEN,
                Instant.parse("2026-09-24T13:00:00Z"));
        outbox = TicketEventOutbox.from(event);
        relay = new TicketEventOutboxRelay(repository, publisher, Duration.ofMillis(100));
        when(repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc())
                .thenReturn(List.of(outbox));
    }

    @Test
    void ack_kafka_menandai_outbox_sebagai_published() {
        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));

        relay.relayPending();

        assertThat(outbox.getPublishedAt()).isNotNull();
        verify(repository).save(outbox);
    }

    @Test
    void kafka_gagal_tetap_pending_lalu_dicoba_ulang() {
        when(publisher.publish(event)).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("Kafka mati")),
                CompletableFuture.completedFuture(null));

        relay.relayPending();

        assertThat(outbox.getPublishedAt()).isNull();
        verify(repository, never()).save(outbox);

        relay.relayPending();

        assertThat(outbox.getPublishedAt()).isNotNull();
        verify(publisher, times(2)).publish(event);
        verify(repository).save(outbox);
    }
}
