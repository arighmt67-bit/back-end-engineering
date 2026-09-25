package com.arirahmat.helpdesk.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketEventDeserializationTest {

    @Test
    void producer_v1_json_contract_is_deserialized_without_java_type_header() throws Exception {
        String payload = """
                {
                  "eventId":"7c2ab773-5fd8-45aa-81fc-931483971cd4",
                  "eventType":"ticket.created.v1",
                  "eventVersion":1,
                  "ticketId":42,
                  "ownerEmail":"owner@example.com",
                  "status":"OPEN",
                  "occurredAt":"2026-09-24T13:00:00Z"
                }
                """;
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        TicketEvent event = mapper.readValue(payload, TicketEvent.class);

        assertThat(event.eventId()).isEqualTo(
                UUID.fromString("7c2ab773-5fd8-45aa-81fc-931483971cd4"));
        assertThat(event.eventType()).isEqualTo(TicketEvent.CREATED);
        assertThat(event.eventVersion()).isEqualTo(TicketEvent.VERSION);
        assertThat(event.ticketId()).isEqualTo(42L);
        assertThat(event.ownerEmail()).isEqualTo("owner@example.com");
        assertThat(event.status()).isEqualTo("OPEN");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-24T13:00:00Z"));
    }
}
