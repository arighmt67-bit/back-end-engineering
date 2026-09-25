package com.arirahmat.helpdesk.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTicketEventPublisher {

    private final KafkaTemplate<String, TicketEvent> kafkaTemplate;
    private final String topic;

    public KafkaTicketEventPublisher(
            KafkaTemplate<String, TicketEvent> kafkaTemplate,
            @Value("${app.kafka.ticket-events-topic:helpdesk.ticket-events.v1}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<?> publish(TicketEvent event) {
        return kafkaTemplate.send(topic, String.valueOf(event.ticketId()), event);
    }
}
