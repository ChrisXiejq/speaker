package com.speaker.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speaker.app.cache.BankCacheNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 题库缓存：默认使用 JVM 内存（本地无 Redis 也可启动）；服务器上设置 {@code app.cache.use-redis=true} 并配置
 * {@code spring.data.redis} 后使用 Redis。
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.cache.use-redis", havingValue = "true")
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${app.cache.bank-ttl-ms:600000}") long bankTtlMs) {
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper.copy());
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(bankTtlMs))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.cache.use-redis", havingValue = "false", matchIfMissing = true)
    public CacheManager simpleBankCacheManager() {
        ConcurrentMapCacheManager m = new ConcurrentMapCacheManager(
                BankCacheNames.BANK_SEASONS, BankCacheNames.BANK_TOPIC_GROUPS);
        m.setAllowNullValues(false);
        return m;
    }
}
