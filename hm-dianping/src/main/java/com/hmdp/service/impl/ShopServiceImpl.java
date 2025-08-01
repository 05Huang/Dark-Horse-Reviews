package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hmdp.utils.RedisData;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static java.lang.Thread.sleep;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

//    public Shop queryWithLogicalExpire(Long id){
//        String redisKey = RedisConstants.CACHE_SHOP_KEY + id;
//        // 1. 从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(redisKey);
//        // 2. 判读是否存在
//        if(StrUtil.isBlank(shopJson)){
//            // 3. 不存在，直接返回
//            return null;
//        }
//        // 4. 命中需要判断过期时间，需要先把json反序列化位对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject jsonData = (JSONObject) redisData.getData(); // 如果不强转就是一个Object，但本质上是JSONObject，所以先转成JSONObject
//        Shop shop = JSONUtil.toBean(jsonData, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        // 5. 判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            // 5.1 未过期，直接返回店铺信息
//            return shop;
//        }
//        // 5.2 已过期，需要缓存重建
//        // 6. 缓存重建
//        // 6.1 获取互斥锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 6.2 判断是否获取锁成功
//        if(isLock){
//            //6.3成功 开启独立线程实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    // 重建缓存
//                    this.saveShop2Redis(id,20L);
//                }catch (Exception e){
//
//                } finally {
//                    // 释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//        // 6.4 返回过期的商铺信息
//        return shop;
//    }

//    public void saveShop2Redis(Long id, Long expireSeconds) {
//        Shop shop = getById(id);
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop =queryWithPassThrough(id);
        //缓存击穿(使用互斥锁)
        Shop shop = queryWithMutex(id);
        //缓存击穿(使用逻辑过期)
       //Shop shop = queryWithLogicalExpire(id);
         //缓存击穿(使用封装的逻辑过期)
        //Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (Objects.isNull(shop)) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    private Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.从redis根据id查数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        Shop shop = null;
        if (StrUtil.isNotBlank(shopJson)) {
            //注意，这里从redis中获取的value是json字符串，需要转换成对象才能返回。
            shop = JSONUtil.toBean(shopJson, Shop.class);
            //3.若存在直接返回数据
            return shop;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        try {
            // 2、缓存未命中，需要重建缓存，判断能否能够获取互斥锁

            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                // 2.1 获取锁失败，已有线程在重建缓存，则休眠重试
                sleep(50);
                queryWithMutex(id);
            }
            // 2.2 获取锁成功，判断缓存是否重建，防止堆积的线程全部请求数据库（所以说双检是很有必要的）
            //1.从redis根据id查数据
            String shopJson2 = stringRedisTemplate.opsForValue().get(key);
            if (Objects.nonNull(shopJson2)) {
                // 缓存命中，直接返回
                return JSONUtil.toBean(shopJson2, Shop.class);
            }
            Thread.sleep(200);
            // 3、从数据库中查询店铺数据，并判断数据库是否存在店铺数据
            shop = this.getById(id);
            if (Objects.isNull(shop)) {
                // 数据库中不存在，缓存空对象（解决缓存穿透），返回失败信息
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            // 4、数据库中存在，重建缓存，响应数据
            // 4.2 数据库中存在，重建缓存，并返回店铺数据
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("发生异常");
        } finally {
            // 5、释放锁（释放锁一定要记得放在finally中，防止死锁）
            unlock(lockKey);
        }
        return shop;
    }

    private Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.从redis根据id查数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        Shop shop = null;
        if (StrUtil.isNotBlank(shopJson)) {
            //注意，这里从redis中获取的value是json字符串，需要转换成对象才能返回。
            shop = JSONUtil.toBean(shopJson, Shop.class);
            //3.若存在直接返回数据
            return shop;
        }
        // 2.2 缓存未命中，判断缓存中查询的数据是否是空字符串(isNotBlank把null和空字符串给排除了)
        if (Objects.nonNull(shopJson)) {
            // 2.2.1 当前数据是空字符串（说明该数据是之前缓存的空对象），直接返回失败信息
            return null;
        }
        // 2.2.2 当前数据是null，则从数据库中查询店铺数据
        shop = this.getById(id);

        // 2.2.2 当前数据是null，则从数据库中查询店铺数据
        shop = this.getById(id);

        // 4、判断数据库是否存在店铺数据
        if (Objects.isNull(shop)) {
            // 4.1 数据库中不存在，缓存空对象（解决缓存穿透），返回失败信息
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4.2 数据库中存在，重建缓存，并返回店铺数据
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回数据
        return shop;
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        boolean b = updateById(shop);
        if (!b) {
            return Result.fail("更新店铺信息失败");
        }
        Boolean delete = stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        if (!delete) {
            return Result.fail("删除店铺信息失败");
        }
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}
