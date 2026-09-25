package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketCacheConfigurationTest {

    @Test
    @DisplayName("Cache ticketById memakai TTL sepuluh menit dari konfigurasi")
    void ticket_cache_memakai_ttl_yang_dikonfigurasi() {
        RedisCacheConfiguration configuration = configuration("10m");

        assertThat(configuration.getTtl()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("Serializer Redis mempertahankan tipe TicketResponse dan nilai Instant")
    void redis_serializer_round_trip_ticket_response() {
        RedisCacheConfiguration configuration = configuration("10m");
        TicketResponse original = new TicketResponse(
                42L,
                "Printer error",
                "Tidak bisa mencetak",
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                "owner@example.com",
                Instant.parse("2026-09-24T13:00:00Z"),
                Instant.parse("2026-09-24T13:05:00Z"),
                null);

        ByteBuffer bytes = configuration.getValueSerializationPair().write(original);
        Object restored = configuration.getValueSerializationPair().read(bytes);

        assertThat(restored).isInstanceOf(TicketResponse.class).isEqualTo(original);
    }

    private RedisCacheConfiguration configuration(String ttl) {
        CacheConfig config = new CacheConfig(new ResilientCacheErrorHandler());
        RedisCacheManager.RedisCacheManagerBuilder builder =
                mock(RedisCacheManager.RedisCacheManagerBuilder.class);
        RedisCacheManagerBuilderCustomizer customizer = config.ticketCacheCustomizer(ttl);

        customizer.customize(builder);

        ArgumentCaptor<RedisCacheConfiguration> configuration =
                ArgumentCaptor.forClass(RedisCacheConfiguration.class);
        verify(builder).withCacheConfiguration(eq("ticketById"), configuration.capture());
        return configuration.getValue();
    }
}
