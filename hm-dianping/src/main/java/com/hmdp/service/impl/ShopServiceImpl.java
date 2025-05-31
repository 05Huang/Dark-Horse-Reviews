package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //1.从redis根据id查数据
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //注意，这里从redis中获取的value是json字符串，需要转换成对象才能返回。
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //3.若存在直接返回数据
            return Result.ok(shop);
        }

        //4.从数据库中根据id查数据
        Shop shop = getById(id);
        //5.若不存在，返回错误信息
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        //6.将数据写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop));
        stringRedisTemplate.expire(CACHE_SHOP_KEY + id, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回数据
        return Result.ok(shop);
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
}
