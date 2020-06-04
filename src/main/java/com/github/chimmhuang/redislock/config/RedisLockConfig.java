package com.github.chimmhuang.redislock.config;

import com.github.chimmhuang.redislock.listener.RedisListener;
import com.github.chimmhuang.redislock.lock.RedisLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * @author Chimm Huang
 */
@Configuration
public class RedisLockConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public RedisListener redisListener(RedisMessageListenerContainer redisMessageListenerContainer) {
        return new RedisListener(redisMessageListenerContainer);
    }

    @Bean
    public RedisLock redLock(RedisTemplate redisTemplate) {
        return new RedisLock(redisTemplate);
    }
}
