package com.zhen.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 商户查询缓存
     * @param id
     * @return
     */
    Result queryShopInfoById(Long id);

    /**
     * 更新商铺，并且删除redis缓存
     * @param shop
     * @return
     */
    Result updateShopById(Shop shop);
}
