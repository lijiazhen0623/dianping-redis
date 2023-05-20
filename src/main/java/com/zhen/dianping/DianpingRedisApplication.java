package com.zhen.dianping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication
public class DianpingRedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianpingRedisApplication.class, args);
    }

}
