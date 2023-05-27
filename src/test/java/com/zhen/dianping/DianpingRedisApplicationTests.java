package com.zhen.dianping;

import com.zhen.dianping.service.impl.ShopServiceImpl;
import com.zhen.dianping.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class DianpingRedisApplicationTests {
    @Resource
    ShopServiceImpl shopService;
    @Resource
    RedisIdWorker redisIdWorker;

    @Test
    void contextLoads() {
        shopService.saveShopToRedis(1L);
    }

    ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Test
    void nextIdTest() throws InterruptedException {
        Runnable run = () -> {
            long id = redisIdWorker.nextId("order");
            System.out.println(id);
        };
        for (int i = 1; i < 100; i++) {
            executorService.submit(run);
        }
        Thread.sleep(5000);
//        long id = redisIdWorker.nextId("order");
//        System.out.println(id);
    }

}
