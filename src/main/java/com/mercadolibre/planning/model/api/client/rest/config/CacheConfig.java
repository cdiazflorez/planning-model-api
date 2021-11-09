package com.mercadolibre.planning.model.api.client.rest.config;

import com.google.common.cache.CacheBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final int TIME_DEFAULT_TO_EXPIRE = 60;

    public static final String CPT_CACHE_NAME = "cpt_by_warehouse";

    @Bean
    @Override
    public CacheManager cacheManager() {

        final SimpleCacheManager cacheManager = new SimpleCacheManager();
        final GuavaCache guavaCache = new GuavaCache(CPT_CACHE_NAME, CacheBuilder
                .newBuilder().expireAfterWrite(TIME_DEFAULT_TO_EXPIRE, TimeUnit.MINUTES).build());
        cacheManager.setCaches(List.of(guavaCache));
        return cacheManager;
    }

    @Override
    public CacheResolver cacheResolver() {
        return null;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return null;
    }
}
