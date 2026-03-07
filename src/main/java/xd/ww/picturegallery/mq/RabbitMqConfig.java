package xd.ww.picturegallery.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    // 交换机
    public static final String AI_EXCHANGE = "ai.exchange";
    // 队列名
    public static final String OUT_PAINTING_QUEUE = "ai.out_painting.queue";
    public static final String TAGGING_QUEUE = "ai.tagging.queue";
    // Routing Keys
    public static final String OUT_PAINTING_KEY = "ai.out_painting.key";
    public static final String TAGGING_KEY = "ai.tagging.key";

    @Bean
    public DirectExchange aiExchange() { return new DirectExchange(AI_EXCHANGE); }

    @Bean
    public Queue outPaintingQueue() { return new Queue(OUT_PAINTING_QUEUE); }
    
    @Bean
    public Queue taggingQueue() { return new Queue(TAGGING_QUEUE); }

    @Bean
    public Binding outPaintingBinding() {
        return BindingBuilder.bind(outPaintingQueue()).to(aiExchange()).with(OUT_PAINTING_KEY);
    }

    @Bean
    public Binding taggingBinding() {
        return BindingBuilder.bind(taggingQueue()).to(aiExchange()).with(TAGGING_KEY);
    }
}