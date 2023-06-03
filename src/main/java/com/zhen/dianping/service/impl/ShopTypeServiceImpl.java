package com.zhen.dianping.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.ShopType;
import com.zhen.dianping.mapper.ShopTypeMapper;
import com.zhen.dianping.service.IShopTypeService;
import com.zhen.dianping.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public Result queryTypeList() {
        //1、查询redis缓存
        List<String> list = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOP_TYPE_KEY, 0, -1);
        //2、命中，直接返回
        if (CollectionUtil.isNotEmpty(list)) {
            List<ShopType> shopTypes = list
                    .stream()
                    .map(item -> JSONUtil.toBean(item, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        //3、未命中，查询数据库
        List<ShopType> shopTypes = query()
                .orderByAsc("sort")
                .list();
        List<String> typeJsonList = shopTypes
                .stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        //4、将数据存入redis
        stringRedisTemplate.opsForList().rightPushAll(RedisConstants.CACHE_SHOP_TYPE_KEY,typeJsonList);
        stringRedisTemplate.expire(RedisConstants.CACHE_SHOP_TYPE_KEY,RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //5、返回数据
        return Result.ok(shopTypes);
    }
}
