package com.github.chimmhuang.redislock.lock;

import com.github.chimmhuang.redislock.listener.RedisListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
     * @param expire   锁过期时间
     * @param timeUnit 时间单位
     */
    public void lock(String key, long expire, TimeUnit timeUnit) {
        //加锁
        if (tryLock(key, expire, timeUnit)) {
            return;
        }

        // 如果没有加上阻塞当前线程，等待锁释放
        waitForLock(key);

        // 再次尝试加锁
        lock(key, expire, timeUnit);
    }

    /**
     * 尝试加锁
     *
     * @return 是否加锁成功
     */
    private boolean tryLock(String key, long expire, TimeUnit timeUnit) {
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(key, Thread.currentThread().getName(), expire, timeUnit);
        return aBoolean != null && aBoolean;
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
        return redisTemplate.delete(key);
    }
}
