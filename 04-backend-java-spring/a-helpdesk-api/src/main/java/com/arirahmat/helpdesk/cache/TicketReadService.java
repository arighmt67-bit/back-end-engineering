package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.exception.ResourceNotFoundException;
import com.arirahmat.helpdesk.repository.TicketRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TicketReadService {

    private final TicketRepository ticketRepository;
    private final CacheManager cacheManager;
    private final CacheErrorHandler cacheErrorHandler;

    public TicketReadService(
            TicketRepository ticketRepository,
            CacheManager cacheManager,
            CacheErrorHandler cacheErrorHandler) {
        this.ticketRepository = ticketRepository;
        this.cacheManager = cacheManager;
        this.cacheErrorHandler = cacheErrorHandler;
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Instant currentRevision = ticketRepository.findUpdatedAtById(id)
                .orElseThrow(() -> notFound(id));
        Cache cache = cacheManager.getCache("ticketById");
        TicketResponse cached = readCache(cache, id);
        if (cached != null && currentRevision.equals(cached.updatedAt())) {
            return cached;
        }

        TicketResponse current = ticketRepository.findByIdWithOwner(id)
                .map(TicketResponse::from)
                .orElseThrow(() -> notFound(id));
        writeCache(cache, id, current);
        return current;
    }

    private TicketResponse readCache(Cache cache, Long id) {
        if (cache == null) {
            return null;
        }
        try {
            return cache.get(id, TicketResponse.class);
        } catch (RuntimeException failure) {
            cacheErrorHandler.handleCacheGetError(failure, cache, id);
            return null;
        }
    }

    private void writeCache(Cache cache, Long id, TicketResponse value) {
        if (cache == null) {
            return;
        }
        try {
            cache.put(id, value);
        } catch (RuntimeException failure) {
            cacheErrorHandler.handleCachePutError(failure, cache, id, value);
        }
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Tiket tidak ditemukan dengan id " + id);
    }
}
