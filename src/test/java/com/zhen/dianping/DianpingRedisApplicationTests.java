package com.zhen.dianping;

import com.zhen.dianping.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class DianpingRedisApplicationTests {
    @Resource
    ShopServiceImpl shopService;
    @Test
    void contextLoads() {
        shopService.saveShopToRedis(1L);
    }

}
