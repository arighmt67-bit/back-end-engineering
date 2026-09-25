package com.arirahmat.helpdesk.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        warn("get", exception, cache, key);
    }

    @Override
    public void handleCachePutError(
            RuntimeException exception, Cache cache, Object key, Object value) {
        warn("put", exception, cache, key);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        warn("evict", exception, cache, key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        warn("clear", exception, cache, null);
    }

    private void warn(String operation, RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache {} gagal; request dilanjutkan. cache={}, key={}, cause={}",
                operation, cache.getName(), key, exception.getMessage());
    }
}
