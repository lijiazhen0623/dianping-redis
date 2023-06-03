package com.zhen.dianping.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author LiJiaZhen
 * @date 2023/5/27 8:43
 */
@Component
public class RedisIdWorker {
    /**
     * 获取特定时间的时间戳
     * long begin_timestamp = LocalDateTime.of(2023, 1, 1, 0, 0).toEpochSecond(ZoneOffset.of("+8"));
     */
    private static final long BEGIN_TIMESTAMP = 1672502400L;

    private static final int COUNT_BITS = 32;
    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        //1、生成时间戳 当前时间时间戳 - 特定时间的时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        //2、从redis中获去自增序列号
        //生成当天的时间格式
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long id = stringRedisTemplate.opsForValue().increment("inc:" + keyPrefix + ":" + now);
        //3、拼接并返回
        return id << COUNT_BITS | timeStamp;
    }
}
