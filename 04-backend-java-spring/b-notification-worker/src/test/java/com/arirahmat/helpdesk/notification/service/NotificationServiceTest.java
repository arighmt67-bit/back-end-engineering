package com.arirahmat.helpdesk.notification.service;

import com.arirahmat.helpdesk.notification.entity.NotificationLog;
import com.arirahmat.helpdesk.notification.event.TicketEvent;
import com.arirahmat.helpdesk.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository repository;

    @Test
    void first_supported_event_is_saved_as_notification() {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event(TicketEvent.CREATED);

        service.process(event);

        ArgumentCaptor<NotificationLog> saved = ArgumentCaptor.forClass(NotificationLog.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getValue().getEventType()).isEqualTo(TicketEvent.CREATED);
        assertThat(saved.getValue().getTicketId()).isEqualTo(42L);
        assertThat(saved.getValue().getRecipient()).isEqualTo("owner@example.com");
        assertThat(saved.getValue().getMessage()).contains("#42").contains("OPEN");
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(event.occurredAt());
        assertThat(saved.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void duplicate_event_id_is_ignored() {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event(TicketEvent.CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(true);

        service.process(event);

        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void concurrent_duplicate_constraint_is_treated_as_idempotent() {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event(TicketEvent.CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(false, true);
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        assertThatCode(() -> service.process(event)).doesNotThrowAnyException();

        verify(repository, org.mockito.Mockito.times(2)).existsByEventId(event.eventId());
    }

    @Test
    void unrelated_integrity_violation_is_not_hidden() {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event(TicketEvent.CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(false, false);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("other constraint");
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenThrow(failure);

        assertThatThrownBy(() -> service.process(event)).isSameAs(failure);
    }

    @Test
    void unsupported_event_type_is_ignored_without_database_lookup(CapturedOutput output) {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event("ticket.deleted.v1", TicketEvent.VERSION);

        service.process(event);

        verify(repository, never()).existsByEventId(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        assertThat(output).contains("Ignoring unsupported ticket event")
                .contains("ticket.deleted.v1");
    }

    @Test
    void unsupported_event_version_is_ignored_without_database_lookup() {
        NotificationService service = new NotificationService(repository);
        TicketEvent event = event(TicketEvent.CREATED, 2);

        service.process(event);

        verify(repository, never()).existsByEventId(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private TicketEvent event(String type) {
        return event(type, TicketEvent.VERSION);
    }

    private TicketEvent event(String type, int version) {
        return new TicketEvent(UUID.randomUUID(), type, version, 42L,
                "owner@example.com", "OPEN", Instant.parse("2026-09-24T13:00:00Z"));
    }
}
