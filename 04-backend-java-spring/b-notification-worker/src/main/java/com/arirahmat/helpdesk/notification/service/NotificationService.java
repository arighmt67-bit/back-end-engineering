package com.arirahmat.helpdesk.notification.service;

import com.arirahmat.helpdesk.notification.entity.NotificationLog;
import com.arirahmat.helpdesk.notification.event.TicketEvent;
import com.arirahmat.helpdesk.notification.repository.NotificationLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationLogRepository repository;

    public NotificationService(NotificationLogRepository repository) {
        this.repository = repository;
    }

    public void process(TicketEvent event) {
        if (!supports(event)) {
            log.warn("Ignoring unsupported ticket event: eventId={}, type={}, version={}",
                    event.eventId(), event.eventType(), event.eventVersion());
            return;
        }
        if (repository.existsByEventId(event.eventId())) {
            return;
        }
        String message = "Ticket #" + event.ticketId() + " status " + event.status();
        try {
            repository.saveAndFlush(new NotificationLog(
                    event.eventId(), event.eventType(), event.ticketId(), event.ownerEmail(),
                    message, event.occurredAt(), Instant.now()));
        } catch (DataIntegrityViolationException exception) {
            if (!repository.existsByEventId(event.eventId())) {
                throw exception;
            }
        }
    }

    private boolean supports(TicketEvent event) {
        return event.eventVersion() == TicketEvent.VERSION
                && (TicketEvent.CREATED.equals(event.eventType())
                || TicketEvent.STATUS_CHANGED.equals(event.eventType()));
    }
}
