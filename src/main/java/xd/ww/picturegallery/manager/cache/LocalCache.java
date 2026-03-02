package xd.ww.picturegallery.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存
 */
@Component
public class LocalCache extends EnhancedCacheChainTemplate {
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    @Autowired
    @Qualifier("redisCache")
    @Override
    public void setNext(EnhancedCacheChainTemplate next) {
        super.setNext(next);
    }

    @Override
    public String getStringValue(String key) {
        return (String) cache.getIfPresent(key);
    }

    @Override
    protected boolean isL2() {
        return false;
    }

    @Override
    public void setStringValue(String key, String value, long expireSeconds) {
        cache.put(key, value);
    }


    @Override
    public void deleteValue(String key) {
        cache.invalidate(key);
    }

    @Override
    public void deleteValues(List<String> keys) {
        cache.invalidateAll(keys);
    }


}
