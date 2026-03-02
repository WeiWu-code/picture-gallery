package xd.ww.picturegallery.manager.cache;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class LocalCacheSyncListener implements MessageListener {

    @Resource
    private LocalCache localCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 1. 解析消息
        CacheSyncMessage cacheMessage = JSONUtil.toBean(new String(message.getBody()), CacheSyncMessage.class);
        log.info("接收到缓存失效消息，Key: {}", cacheMessage.getKeys());
        // 2. 清除本地缓存
        localCache.deleteValues(cacheMessage.getKeys());
    }
}