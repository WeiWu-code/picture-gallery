package xd.ww.picturegallery.manager.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * redis缓存
 */
@Component
public class RedisCache extends EnhancedCacheChainTemplate {

    private static final TimeUnit SECONDS = TimeUnit.SECONDS;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    // 结束链
    public void setNext(EnhancedCacheChainTemplate next) {
        super.setNext(null);
    }


    @Override
    public void deleteValue(String key) {
        stringRedisTemplate.delete(key);
    }


    @Override
    public String getStringValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    protected boolean isL2() {
        return true;
    }

    @Override
    public void setStringValue(String key, String value, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expireSeconds, SECONDS);
    }
}
