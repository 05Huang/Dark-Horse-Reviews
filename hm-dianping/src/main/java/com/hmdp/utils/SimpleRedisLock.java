package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @Author: HuangXuan
 * @CreateTime: 2025-06-02
 * @Description:
 * @email hxtxyydsa@gmail.com; 3441578327@qq.com
 * @Version: 1.0
 */


public class SimpleRedisLock {
    private StringRedisTemplate stringRedisTemplate;
    private String name;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static final String KEY_PREFIX = "lock:";

    public boolean tryLock(Long timeSec) {
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, "1", timeSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    public void unlock() {
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
