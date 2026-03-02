package xd.ww.picturegallery.manager.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    public static final String CACHE_CHANNEL = "CACHE_INVALIDATE_TOPIC";

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 订阅频道
        container.addMessageListener(listenerAdapter, new ChannelTopic(CACHE_CHANNEL));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(LocalCacheSyncListener subscriber) {
        return new MessageListenerAdapter(subscriber);
    }
}