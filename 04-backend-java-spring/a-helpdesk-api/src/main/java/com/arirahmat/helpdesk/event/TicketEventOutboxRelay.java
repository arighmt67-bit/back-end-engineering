package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.entity.TicketEventOutbox;
import com.arirahmat.helpdesk.repository.TicketEventOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TicketEventOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(TicketEventOutboxRelay.class);

    private final TicketEventOutboxRepository repository;
    private final KafkaTicketEventPublisher publisher;
    private final Duration acknowledgementTimeout;

    public TicketEventOutboxRelay(
            TicketEventOutboxRepository repository,
            KafkaTicketEventPublisher publisher,
            @Value("${app.kafka.outbox.ack-timeout:5s}") Duration acknowledgementTimeout) {
        this.repository = repository;
        this.publisher = publisher;
        this.acknowledgementTimeout = acknowledgementTimeout;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval:1000}")
    public void relayPending() {
        for (TicketEventOutbox outbox :
                repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                publisher.publish(outbox.toEvent()).get(
                        acknowledgementTimeout.toMillis(), TimeUnit.MILLISECONDS);
                outbox.markPublished(Instant.now());
                repository.save(outbox);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                log.warn("Outbox relay interrupted. eventId={}", outbox.getEventId());
                return;
            } catch (Exception failure) {
                log.warn("Outbox event belum ter-ACK; akan dicoba ulang. eventId={}, cause={}",
                        outbox.getEventId(), failure.getMessage());
            }
        }
    }
}
