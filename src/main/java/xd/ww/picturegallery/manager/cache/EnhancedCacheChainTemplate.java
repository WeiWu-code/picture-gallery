package xd.ww.picturegallery.manager.cache;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 增强版多级缓存模板：
 * 1. 布隆过滤器防止缓存穿透
 * 2. 空值缓存（NULL_OBJECT）避免回源轰炸
 * 3. 延迟双删 + 删除重试，保证最终一致性
 *
 * @author  wei
 */
@Slf4j
public abstract class EnhancedCacheChainTemplate {

    /** 空值缓存的 JSON 占位字符串，业务不可能出现的值 */
    public static final String NULL_OBJECT = "\"__NULL__\"";

    /** 延迟双删队列在 Redis 中的 zset key */
    private static final String DELAY_QUEUE_KEY = "cache:delay:delete";

    @Resource
    private RedissonClient redissonClient;

    // Redisson实现的布隆过滤器
    private RBloomFilter<String> bloomFilter = null;

    /** 线程池，用于扫描延迟队列并执行第二次删除，或者删除重试 */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "cache-scheduler-" + r.hashCode());
                t.setDaemon(true); // 设置为守护线程，防止阻碍 JVM 关闭
                return t;
            });

    /** 下游缓存节点，null 表示已到链尾 */
    @Setter
    protected EnhancedCacheChainTemplate next;

    /** 注入模板，供子类直接操作 Redis */
    @Resource
    protected StringRedisTemplate stringRedisTemplate;

    // 初始化布隆过滤器
    @PostConstruct
    private void init(){
        if(isL2()){
            this.bloomFilter = redissonClient.getBloomFilter("cache:bloom:filter");
            this.bloomFilter.tryInit(1_000_000L, 0.01);
            startDelayDeleteTask();
        }
    }

    /**
     * 从当前节点获取字符串值
     *
     * @param key 缓存键
     * @return 值，若不存在返回 null
     */
    public abstract String getStringValue(String key);

    protected abstract boolean isL2();

    /**
     * 向当前节点写入字符串值，并指定过期时间
     *
     * @param key         缓存键
     * @param value       缓存值
     * @param expireSeconds 过期秒数
     */
    public abstract void setStringValue(String key, String value, long expireSeconds);


    /**
     * 从当前节点删除单个 key
     *
     * @param key 缓存键
     */
    public abstract void deleteValue(String key);

    /**
     * 链式获取字符串值：
     * 1. 布隆过滤器拦截非法 keyA
     * 2. 当前节点命中（含空值）直接返回
     * 3. 下游节点命中后回填当前节点并返回
     * 4. 全链路未命中则写入空值缓存并返回 null
     *
     * @param key 缓存键
     * @return 值，全链路未命中返回 null
     */
    public final String getStringValueChain(String key) {
        // 布隆过滤器不存在则直接返回 NULL_OBJECT，防止缓存穿透
        if (bloomFilter != null && !bloomFilter.contains(key)) {
            return NULL_OBJECT;
        }

        // 当前节点命中
        String val = getStringValue(key);
        if (val != null) {
            return val;
        }

        // 下游继续查
        if (next != null) {
            String down = next.getStringValueChain(key);
            if (down != null) {
                // 回填当前节点，过期时间抖动 300~600s
                setStringValue(key, down, randomExpire());
                return down;
            }
        }
        return null;
    }

    // 同上，但是不用布隆过滤器
    public final String getStringValueChainWithoutBloom(String key) {

        // 当前节点命中
        String val = getStringValue(key);
        if (val != null) {
            return val;
        }

        // 下游继续查
        if (next != null) {
            String down = next.getStringValueChain(key);
            if (down != null) {
                // 回填当前节点，过期时间抖动 300~600s
                setStringValue(key, down, randomExpire());
                return down;
            }
        }
        return null;
    }

    /**
     * 链式写入：
     * 1. 写入布隆过滤器
     * 2. 当前节点写入
     * 3. 递归写入下游
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public final void setStringValueChain(String key, String value) {
        if(bloomFilter != null){
            bloomFilter.add(key); // 保证布隆过滤器先存在
        }
        setStringValue(key, value, randomExpire());
        if (next != null) {
            next.setStringValueChain(key, value);
        }
    }

    // 同上，不使用布隆过滤器的版本
    public final void setStringValueChainWithoutBloom(String key, String value) {
        setStringValue(key, value, randomExpire());
        if (next != null) {
            next.setStringValueChain(key, value);
        }
    }

    /**
     * 链式删除单个 key：
     * 1. 同步删除（带重试）
     * 2. 下游递归删除
     * 3. 向延迟队列投递“二次删除”任务
     *
     * @param key 缓存键
     */
    public final void deleteValueChain(String key) {
        safeDelete(key); // 第一次删除
        if (next != null) {
            next.deleteValueChain(key);
        }else{
            // 延迟双删：5 秒后再次删除，防止并发写脏
            long delayMillis = System.currentTimeMillis() + 5_000;
            stringRedisTemplate.opsForZSet().add(DELAY_QUEUE_KEY, key, delayMillis);
        }
    }


    /**
     * 工具方法：把 JSON 字符串反序列成对象，自动识别空值占位
     *
     * @param key  缓存键
     * @param type 类型引用
     * @param <T>  目标类型
     * @param isBloom 是否需要布隆过滤器
     * @return 反序列化对象，若命中空值占位则返回 null
     */
    public <T> CacheResult<T> getObjectValueForString(String key, TypeReference<T> type, Boolean isBloom) {
        // 如果需要布隆过滤器
        String val = null;
        if(isBloom){
            val = getStringValueChain(key);
        }else{
            val = getStringValueChainWithoutBloom(key);
        }
        if (val == null) {
            return CacheResult.miss();
        }
        if(NULL_OBJECT.equals(val)){
            return CacheResult.hit(null);
        }
        return CacheResult.hit(JSONUtil.toBean(val, type, true));
    }


    /**
     * 带指数退避的删除重试，最多 3 次
     * 重试时，要异步
     * @param key 缓存键
     */
    private void safeDelete(String key) {
        try {
            deleteValue(key); // 第一次尝试
        } catch (Exception e) {
            log.warn("Direct delete failed for key: {}, submitting async retry...", key);
            // 提交异步重试任务，第 1 次重试
            if(isL2())
            {
                submitAsyncRetry(key, 1);
            }
        }
    }

    /**
     * 在基础时间上增加 0~300 秒的随机抖动，防止大量 key 同时过期
     *
     * @return 实际过期秒数
     */
    private long randomExpire() {
        return (long) 300 + (long) (Math.random() * 300);
    }


    /**
     * Spring 依赖注入完成后启动延迟队列消费者
     */

    private void startDelayDeleteTask() {
        SCHEDULER.scheduleWithFixedDelay(() -> {
            try {
                Set<String> keys = stringRedisTemplate.opsForZSet()
                        .rangeByScore(DELAY_QUEUE_KEY, 0, System.currentTimeMillis(), 0, 100);
                if (keys != null && !keys.isEmpty()) {
                    for (String k : keys) {
                        stringRedisTemplate.delete(k);           // 二次删除
                        stringRedisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, k);
                        log.debug("delay second delete key={}", k);
                    }
                }
            } catch (Exception e) {
                log.error("delay delete task error", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }


    /**
     * 线程池销毁
     * Spring 容器销毁 Bean 时会自动调用此方法
     */
    @PreDestroy
    public void destroy() {
        log.info("Closing cache scheduler...");
        if(!isL2()) return;
        try {
            // 1. 停止接收新任务
            SCHEDULER.shutdown();

            // 2. 等待现有任务执行完成（最多等 20 秒）
            if (!SCHEDULER.awaitTermination(20, TimeUnit.SECONDS)) {
                // 3. 如果超时还没完，强制关闭
                log.warn("Scheduler did not terminate in time, forcing shutdown.");
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            // 捕获异常，重新设置中断状态
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Cache scheduler closed.");
    }


    /**
     * 递归提交重试任务
     */
    private void submitAsyncRetry(String key, int retryCount) {
        if (retryCount > 3) {
            log.error("Async delete finally failed after 3 retries for key: {}", key);
            return;
        }

        // 计算退避时间
        long delay = 100L * (1L << retryCount);
        SCHEDULER.schedule(() -> {
            try {
                deleteValue(key);
                log.info("Async retry success for key: {}", key);
            } catch (Exception e) {
                log.warn("Async retry {} failed for key: {}", retryCount, key);
                // 再次失败，提交下一次重试
                submitAsyncRetry(key, retryCount + 1);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
}