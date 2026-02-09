package xd.ww.picturegallery.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 缓存版本管理器
 * 用于生成逻辑版本号，控制列表缓存的整体失效
 */
@Component
@Slf4j
public class CacheVersionManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 分页列表的版本号 Key
    private static final String PICTURE_LIST_VERSION_KEY = "smartGalleryHub:version:listPicture";

    // 本地缓存版本号（只存 5 秒），防止每次都查 Redis
    private final Cache<String, String> localVersionCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS) // 5秒后本地过期，去 Redis 拿最新版本
            .maximumSize(100)
            .build();

    /**
     * 获取当前列表缓存的版本号
     * 流程：L1 -> L2 -> Default("1")
     */
    public String getPictureListVersion() {
        // 1. 查本地 L1
        String version = localVersionCache.getIfPresent(PICTURE_LIST_VERSION_KEY);
        if (version != null) {
            return version;
        }

        // 2. 查 Redis L2
        version = stringRedisTemplate.opsForValue().get(PICTURE_LIST_VERSION_KEY);
        
        // 3. 初始化默认值
        if (version == null) {
            version = "1";
            stringRedisTemplate.opsForValue().set(PICTURE_LIST_VERSION_KEY, version, 24, TimeUnit.HOURS);
        }

        // 4. 回写本地 L1
        localVersionCache.put(PICTURE_LIST_VERSION_KEY, version);
        return version;
    }

    /**
     * 升级版本号（相当于让旧缓存失效）
     * 流程：Redis INCR -> 清除本地 L1
     */
    public void refreshPictureListVersion() {
        // 1. Redis 原子自增
        Long newVersion = stringRedisTemplate.opsForValue().increment(PICTURE_LIST_VERSION_KEY);
        log.info("图片列表缓存版本升级: v{}", newVersion);

        // 2. 清除本地缓存（让当前节点立即感知）
        // 注意：其他节点的 L1 需要等 5 秒自动过期，或者你可以用 Redis 广播通知它们
        localVersionCache.invalidate(PICTURE_LIST_VERSION_KEY);
    }
}