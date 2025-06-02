package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @Author: HuangXuan
 * @CreateTime: 2025-06-02
 * @Description:
 * @email hxtxyydsa@gmail.com; 3441578327@qq.com
 * @Version: 1.0
 */

@Slf4j
@Component
public class CacheClient {


    private final StringRedisTemplate stringRedisTemplate;

    // 用构造器注入
    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }


//    函数式编程：在封装queryWithPassThrough的时候，里面在redis查询不存在的时候，我们要去查询数据库，那查询数据库的代码，
//    我们泛型传递的参数，调用哪个查询数据库的函数去查询数据库呢？这时要用函数式编程，把要用到的函数通过参数传递过来，有参数有
//    返回值就用Function<ID, R> dbFallback，使用的时候直接R r = dbFallback.apply(id);即可，调用这个工具方法的时候
//    把具体的查询函数作为参数传进去。
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String redisKey = keyPrefix + id;
        // 1. 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        // 2. 判读是否存在
        if(StrUtil.isNotBlank(json)){
            // 3. 存在，直接返回
            return JSONUtil.toBean(json, type);
        }

        // 命中的是否是空值
        if(json != null){
            // 返回一个错误信息
            return null;
        }

        // 4. 不存在，根据id查询数据库------我们哪知道去查哪个数据库，只能调用者告诉我们，------函数式编程
        R r = dbFallback.apply(id);
        // 5. 不存在，返回错误
        if(r == null){
            // 将空值写入redis------解决缓存穿透
            stringRedisTemplate.opsForValue().set(redisKey,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6. 存在，写入redis
        this.set(redisKey, r, time, unit);
        // 7. 返回
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String redisKey = keyPrefix + id;
        // 1. 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        // 2. 判读是否存在
        if(StrUtil.isBlank(json)){
            // 3. 不存在，直接返回
            return null;
        }
        // 4. 命中需要判断过期时间，需要先把json反序列化位对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonData = (JSONObject) redisData.getData(); // 如果不强转就是一个Object，但本质上是JSONObject，所以先转成JSONObject
        R r = JSONUtil.toBean(jsonData, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5. 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 5.1 未过期，直接返回店铺信息
            return r;
        }
        // 5.2 已过期，需要缓存重建
        // 6. 缓存重建
        // 6.1 获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 判断是否获取锁成功
        if(isLock){
            //6.3成功 开启独立线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 重建缓存
                    // 先查数据库
                    R r1 = dbFallback.apply(id);
                    // 写入redis
                    this.setWithLogicalExpire(redisKey, r1, time, unit);
                }catch (Exception e){

                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }
        // 6.4 返回过期的商铺信息
        return r;
    }


    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);  // 因为flag是封装类，而要求的返回值是基本数据类型，在返回的时候就会进行自动的拆箱，拆箱的时候会出现空指针
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


}
