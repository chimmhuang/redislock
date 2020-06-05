package com.github.chimmhuang.redislock.lock;

import com.github.chimmhuang.redislock.listener.RedisListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * 基于 redisTemplate 的 redis 分布式锁
 * Redis distributed lock based on redisTemplate
 *
 * @author Chimm Huang
 */
public class RedisLock {

    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    private RedisTemplate redisTemplate;

    public RedisLock(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 加锁
     *
     * @param key      锁名称
     * @param expire   锁过期时间（单位：秒）
     */
    public void lock(String key, long expire) {
        //加锁
        if (tryLock(key, expire)) {
            return;
        }

        // 如果没有加上阻塞当前线程，等待锁释放
        waitForLock(key);

        // 再次尝试加锁
        lock(key, expire);
    }

    /**
     * 尝试加锁
     *
     * @return 是否加锁成功
     */
    private boolean tryLock(String key, long expire) {

        String script = "if redis.call('setNx',KEYS[1],ARGV[1]) then if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('expire',KEYS[1],ARGV[2]) else return 0 end end";

        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        Object result = redisTemplate.execute(redisScript, new StringRedisSerializer(), new StringRedisSerializer(), Collections.singletonList(key), Thread.currentThread().getName(), expire + "");

        return 1 == Integer.parseInt(result.toString());
    }

    /**
     * 等待锁释放
     * Wait for the lock to be released
     *
     * @param key redis lock key
     */
    private void waitForLock(String key) {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        RedisListener.addCountDownLatch(key, countDownLatch);

        try {
            countDownLatch.await();
        } catch (Exception e) {
            log.error("Error releasing thread : {}", e.getMessage(), e);
        }
    }


    /**
     * 解锁
     */
    public boolean unlock(String key) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Object result = redisTemplate.execute(redisScript, new StringRedisSerializer(), new StringRedisSerializer(), Collections.singletonList(key), Thread.currentThread().getName());
        return 1 == Integer.parseInt(result.toString());
    }
}
