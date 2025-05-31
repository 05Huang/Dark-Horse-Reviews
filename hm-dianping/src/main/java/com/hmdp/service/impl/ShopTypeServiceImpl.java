package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        //1.从redis查数据
        String shopTypeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopTypeJson)) {
            //注意，这里从redis中获取的value是json字符串，需要转换成对象才能返回。
            ShopType shopType = JSONUtil.toBean(shopTypeJson, ShopType.class);
            //3.若存在直接返回数据
            return Result.ok(shopType);
        }
        //4.从数据库中根据id查数据
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        //5.若不存在，返回错误信息
        if (shopTypes == null || shopTypes.isEmpty()) {
            return Result.fail("店铺不存在");
        }
        // 3.2.数据库中存在，则将查询到的信息存入 Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY, JSONUtil.toJsonStr(shopTypes));
        // 3.3返回
        return Result.ok(shopTypes);
    }
}
