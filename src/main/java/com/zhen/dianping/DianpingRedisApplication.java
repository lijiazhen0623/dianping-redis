package com.zhen.dianping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @EnableAspectJAutoProxy(exposeProxy = true) 暴露AOP的Proxy对象
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableTransactionManagement
@SpringBootApplication
public class DianpingRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianpingRedisApplication.class, args);
    }

}
