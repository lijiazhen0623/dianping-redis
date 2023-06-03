package com.zhen.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.ShopType;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 店铺类型查询缓存
     * @return
     */
    Result queryTypeList();
}
