package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.entity.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaTicketEventPublisherTest {

    @Mock
    private KafkaTemplate<String, TicketEvent> kafkaTemplate;

    @Test
    void publish_menggunakan_ticket_id_sebagai_key_dan_mengembalikan_ack_future() {
        TicketEvent event = event();
        CompletableFuture<SendResult<String, TicketEvent>> acknowledgement =
                new CompletableFuture<>();
        when(kafkaTemplate.send("helpdesk.ticket-events.v1", "42", event))
                .thenReturn(acknowledgement);
        KafkaTicketEventPublisher publisher = new KafkaTicketEventPublisher(
                kafkaTemplate, "helpdesk.ticket-events.v1");

        CompletableFuture<?> result = publisher.publish(event);

        assertThat(result).isSameAs(acknowledgement);
        verify(kafkaTemplate).send("helpdesk.ticket-events.v1", "42", event);
    }

    private TicketEvent event() {
        return new TicketEvent(UUID.randomUUID(), TicketEvent.CREATED, TicketEvent.VERSION, 42L,
                "owner@example.com", TicketStatus.OPEN,
                Instant.parse("2026-09-24T13:00:00Z"));
    }
}
