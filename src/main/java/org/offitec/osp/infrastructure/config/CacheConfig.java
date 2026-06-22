package org.offitec.osp.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Read-through caching for the public unit endpoints. The unit detail and
 * calculation payloads are user-independent and read-heavy, so they are cached
 * by unit id. Entries are evicted wholesale whenever an admin mutates a unit or
 * its assets (see {@code UnitAppService}); admin writes are rare, so clearing the
 * whole cache on a write is simpler and safer than per-key eviction.
 *
 * An in-memory {@link ConcurrentMapCacheManager} is enough here: the cached set is
 * bounded by the number of units, and a single app instance serves these reads.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String UNIT_DETAIL = "unitDetail";
    public static final String UNIT_CALC_DATA = "unitCalcData";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(UNIT_DETAIL, UNIT_CALC_DATA);
    }
}
