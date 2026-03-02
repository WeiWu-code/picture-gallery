package xd.ww.picturegallery.manager.cache;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static xd.ww.picturegallery.manager.cache.CacheConstant.DELETE_CACHE;
import static xd.ww.picturegallery.manager.cache.RedisPubSubConfig.CACHE_CHANNEL;

/**
 * redis缓存
 */
@Component
public class RedisCache extends EnhancedCacheChainTemplate {

    private static final String CLIENT_ID = UUID.randomUUID().toString();

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
        CacheSyncMessage msg = new CacheSyncMessage(CLIENT_ID, DELETE_CACHE,List.of(key));
        stringRedisTemplate.convertAndSend(CACHE_CHANNEL, JSONUtil.toJsonStr(msg));
    }

    @Override
    public void deleteValues(List<String> keys) {
        stringRedisTemplate.delete(keys);
        CacheSyncMessage msg = new CacheSyncMessage(CLIENT_ID, DELETE_CACHE, keys);
        stringRedisTemplate.convertAndSend(CACHE_CHANNEL, JSONUtil.toJsonStr(msg));
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
        CacheSyncMessage msg = new CacheSyncMessage(CLIENT_ID, DELETE_CACHE,List.of(key));
        stringRedisTemplate.convertAndSend(CACHE_CHANNEL, JSONUtil.toJsonStr(msg));
    }
}
