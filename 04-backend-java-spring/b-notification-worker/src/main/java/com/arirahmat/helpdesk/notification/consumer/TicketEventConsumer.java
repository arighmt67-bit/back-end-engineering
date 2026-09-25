package com.arirahmat.helpdesk.notification.consumer;

import com.arirahmat.helpdesk.notification.event.TicketEvent;
import com.arirahmat.helpdesk.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketEventConsumer {

    private final NotificationService notificationService;

    public TicketEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.ticket-events-topic}")
    public void consume(TicketEvent event) {
        notificationService.process(event);
    }
}
