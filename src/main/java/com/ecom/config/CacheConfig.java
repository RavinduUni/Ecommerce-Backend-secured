package com.ecom.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on @Cacheable / @CacheEvict. The cache itself (Caffeine, 30 s, 2000 entries)
 * is configured under spring.cache in application.yml.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Every cache that holds product data; all are cleared when a product changes. */
    public static final String CATALOG = "catalog";
    public static final String CATEGORIES = "categories";
    public static final String PRODUCT = "product";
    public static final String ACTIVE_PRODUCTS = "activeProducts";
}