package com.arirahmat.helpdesk.notification.consumer;

import com.arirahmat.helpdesk.notification.event.TicketEvent;
import com.arirahmat.helpdesk.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketEventConsumerTest {

    @Test
    void consumed_event_is_delegated_unchanged() {
        NotificationService service = mock(NotificationService.class);
        TicketEventConsumer consumer = new TicketEventConsumer(service);
        TicketEvent event = new TicketEvent(UUID.randomUUID(), TicketEvent.CREATED,
                TicketEvent.VERSION, 42L, "owner@example.com", "OPEN", Instant.now());

        consumer.consume(event);

        verify(service).process(event);
    }
}
