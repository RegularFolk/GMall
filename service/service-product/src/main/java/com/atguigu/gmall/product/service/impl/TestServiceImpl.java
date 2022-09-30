package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /*
     * 这种策略在redis集群环境下会失效
     * */
    public void testLock1() {
        String uuid = UUID.randomUUID().toString();
        boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 1, TimeUnit.SECONDS);
        if (lock) {
            String num = redisTemplate.opsForValue().get("num");
            if (!StringUtils.isBlank(num)) {
                int integer = Integer.parseInt(num);
                redisTemplate.opsForValue().set("num", String.valueOf(++integer));
            }
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Collections.singletonList("lock"), uuid);
        } else {
            try {//停止一毫秒后重试
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void testLock() {
        String skuId = "25";
        String lockKey = "lock" + skuId;
        RLock lock = redissonClient.getLock(lockKey);//Rlock 表示Reentrant Lock，可重入锁
        lock.lock(10, TimeUnit.SECONDS);//10秒后自动释放或者提前手动释放
        String value = redisTemplate.opsForValue().get("num");
        if (!StringUtils.isBlank(value)) {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
            lock.unlock();
        }
    }

    @Override
    public String readLock() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.readLock();
        rLock.lock(10, TimeUnit.SECONDS);
        return redisTemplate.opsForValue().get("msg");
    }

    @Override
    public String writeLock() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.writeLock();
        rLock.lock(10, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());
        return "成功写入了内容。。。。";
    }
}
