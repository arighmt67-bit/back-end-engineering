package com.arirahmat.helpdesk.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientCacheErrorHandlerTest {

    @Test
    @DisplayName("Semua kegagalan operasi cache dicatat tanpa menggagalkan request")
    void semua_cache_error_tidak_dilempar_ulang() {
        ResilientCacheErrorHandler handler = new ResilientCacheErrorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("ticketById");
        RuntimeException failure = new IllegalStateException("Redis tidak tersedia");

        assertThatCode(() -> handler.handleCacheGetError(failure, cache, 10L))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(failure, cache, 10L, "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(failure, cache, 10L))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(failure, cache))
                .doesNotThrowAnyException();
    }
}
