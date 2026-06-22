package org.offitec.osp.infrastructure.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Clears the cached public unit detail/calc payloads. Put on admin write methods that
 * change a unit or its assets. Admin writes are infrequent, so evicting everything is
 * simpler and safer than trying to evict the exact affected unit id (a single edit can
 * touch many derived fields, and the catalog is small).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(value = CacheConfig.UNIT_DETAIL, allEntries = true),
        @CacheEvict(value = CacheConfig.UNIT_CALC_DATA, allEntries = true)
})
public @interface EvictsUnitCaches {
}
