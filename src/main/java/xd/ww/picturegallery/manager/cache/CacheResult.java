package xd.ww.picturegallery.manager.cache;

import lombok.Getter;

@Getter
public class CacheResult<T> {
    private final T data;       // 实际的数据
    private final boolean hit;  // 是否命中了缓存（包括命中了 __NULL__）

    private CacheResult(T data, boolean hit) {
        this.data = data;
        this.hit = hit;
    }

    // 静态工厂方法
    public static <T> CacheResult<T> hit(T data) { return new CacheResult<>(data, true); }
    public static <T> CacheResult<T> miss() { return new CacheResult<>(null, false); }

}