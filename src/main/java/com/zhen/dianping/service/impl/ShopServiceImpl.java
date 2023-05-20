package com.zhen.dianping.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.Shop;
import com.zhen.dianping.mapper.ShopMapper;
import com.zhen.dianping.service.IShopService;
import com.zhen.dianping.utils.CacheClient;
import com.zhen.dianping.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryShopInfoById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        Shop shop = cacheClient.queryWithMutex(key, id, Shop.class
                , this::getById,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        Shop shop = cacheClient.queryWithLogicalExpire(key, id, Shop.class
//                , this::getById, 20L, TimeUnit.SECONDS);
        return Result.ok(shop);
    }

    /**
     * 数据预热
     * @param id
     */
    public void saveShopToRedis(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        Shop shop = getById(id);
        cacheClient.setWithLogicalExpire(key,shop,20L,TimeUnit.SECONDS);
    }

    @Transactional
    @Override
    public Result updateShopById(Shop shop) {
        //1、更新数据库
        if (shop == null || shop.getId() == null) {
            return Result.fail("参数错误");
        }
        updateById(shop);
        //2、删除对应的redis缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
