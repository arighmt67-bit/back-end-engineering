package com.arirahmat.helpdesk.event;

import com.arirahmat.helpdesk.entity.TicketStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketEventSerializationTest {

    @Test
    @DisplayName("Payload Kafka memakai timestamp ISO-8601 sesuai kontrak v1")
    void wire_payload_memakai_timestamp_iso_8601() throws Exception {
        TicketEvent event = new TicketEvent(
                UUID.fromString("7c2ab773-5fd8-45aa-81fc-931483971cd4"),
                TicketEvent.CREATED,
                TicketEvent.VERSION,
                42L,
                "owner@example.com",
                TicketStatus.OPEN,
                Instant.parse("2026-09-24T13:00:00Z"));
        byte[] payload;
        try (JsonSerializer<TicketEvent> serializer = new JsonSerializer<>()) {
            payload = serializer.serialize("helpdesk.ticket-events.v1", event);
        }

        JsonNode json = new ObjectMapper().readTree(payload);

        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "eventId", "eventType", "eventVersion", "ticketId",
                "ownerEmail", "status", "occurredAt");
        assertThat(json.path("occurredAt").isTextual()).isTrue();
        assertThat(json.path("occurredAt").textValue()).isEqualTo("2026-09-24T13:00:00Z");
    }
}
