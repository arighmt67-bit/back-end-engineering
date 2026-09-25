package com.arirahmat.helpdesk.cache;

import com.arirahmat.helpdesk.dto.TicketResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private final ResilientCacheErrorHandler cacheErrorHandler;

    public CacheConfig(ResilientCacheErrorHandler cacheErrorHandler) {
        this.cacheErrorHandler = cacheErrorHandler;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler;
    }

    @Bean
    RedisCacheManagerBuilderCustomizer ticketCacheCustomizer(
            @Value("${app.cache.ticket-ttl:10m}") String ticketTtl) {
        Duration ttl = DurationStyle.detectAndParse(ticketTtl);
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        Jackson2JsonRedisSerializer<TicketResponse> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, TicketResponse.class);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return builder -> builder.withCacheConfiguration("ticketById", configuration);
    }
}
