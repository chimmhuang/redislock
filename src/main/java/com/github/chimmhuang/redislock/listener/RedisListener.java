package com.github.chimmhuang.redislock.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.KeyspaceEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Listen for redis key events
 *
 * @author Chimm Huang
 */
public class RedisListener extends KeyspaceEventMessageListener {

    private static ConcurrentHashMap<CountDownLatch, String> countDownLatchMap = new ConcurrentHashMap<>();

    /**
     * 将线程与key进行关联，并添加到容器中
     * Associate the countDownLatch with the lock-key and add it to the container
     *
     * @param lockKey redis的锁钥匙 redis lock key
     * @param countDownLatch 多线程计数器
     */
    public static void addCountDownLatch(String lockKey, CountDownLatch countDownLatch) {
        countDownLatchMap.put(countDownLatch, lockKey);
    }

    public RedisListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * Handle redis key events
     *
     * @see MessageListener
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        Set<Entry<CountDownLatch, String>> entrySet = countDownLatchMap.entrySet();
        for (Entry<CountDownLatch, String> entry : entrySet) {
            if (message.toString().equals(entry.getValue())) {
                CountDownLatch countDownLatch = entry.getKey();
                countDownLatch.countDown();
                countDownLatchMap.remove(countDownLatch);
            }
        }
    }

    @Override
    protected void doHandleMessage(Message message) {
        // Use the onMessage method to handle
    }
}
