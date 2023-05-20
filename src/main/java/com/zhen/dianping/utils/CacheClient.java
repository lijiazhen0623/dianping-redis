package com.zhen.dianping.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author LiJiaZhen
 * @date 2023/5/20 10:25
 */
@Slf4j
@Component
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试设置锁
     * @param key 锁名
     * @return
     */
    public boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key 锁名
     * @return
     */
    public boolean unlock(String key){
        Boolean flag = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 将对象转换为json，插入redis
     * @param key
     * @param value
     * @param timeout
     * @param unit
     */
    public void set(String key,Object value,Long timeout,TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),timeout,unit);
    }

    /**
     * 逻辑过期
     * @param key
     * @param value
     * @param timeout
     * @param unit
     */
    public void setWithLogicalExpire(String key,Object value,Long timeout,TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    /**
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dbFallback
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }

    /**
     * 逻辑过期解决缓存击穿，前提是需要数据预热
     * @param key
     * @param id
     * @param type
     * @param dbFallback
     * @param timeout
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithLogicalExpire(String key
            , ID id
            , Class<R> type
            , Function<ID,R> dbFallback
            ,Long timeout
            ,TimeUnit unit) {
        //1、从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2、判断缓存是否命中
        if (StrUtil.isBlank(json)) {
            //未命中直接返回空
            return null;
        }
        //命中，将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = BeanUtil.toBean(redisData.getData(), type);
        //3、判断是否过期
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期
            return r;
        }
        //过期，获取互斥锁
        boolean lock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
        if (!lock) {
            //未获得锁，返回过期数据
            return r;
        }
        //开启独立线程，更新缓存
        CACHE_REBUILD_EXECUTOR.submit(()->{
            try {
                //查询数据库
                R r1 = dbFallback.apply(id);
                //更新缓存
                setWithLogicalExpire(key, r1, timeout, unit);
            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                //释放锁
                unlock(RedisConstants.LOCK_SHOP_KEY + id);
            }
        });
        //返回过期数据
        return r;
    }

    /**
     * 互斥锁解决缓存击穿
     * @param key
     * @param id
     * @param type
     * @param dbFallback
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID> R queryWithMutex(String key
            , ID id
            , Class<R> type
            , Function<ID,R> dbFallback
            ,Long timeout
            ,TimeUnit unit) {
        //1、从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2、判断缓存是否命中
        if (StrUtil.isNotBlank(json)) {
            //3、命中，直接返回
            R r = JSONUtil.toBean(json, type);
            return r;
        }
        //2、解决缓存穿透  json = ""
        if(json != null){
            return null;
        }
        //4、未命中，获取互斥锁
        //4.1 判断是否获取到锁
        try {
            boolean lock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
            //否
            if (!lock) {
                //休眠一段时间
                Thread.sleep(50);
                //跳转到查redis
                return queryWithMutex(key, id, type, dbFallback,timeout,unit);
            }
            //是
            //再次检测redis缓存是否存在，做二次检测
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                R r = JSONUtil.toBean(json, type);
                //释放锁
                unlock(key);
                return r;
            }
            //5 从数据库中查询数据
            R r = dbFallback.apply(id);
            if (r == null) {
                //1、解决缓存穿透
                set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                //5.1 不存在
                return null;
            }
            //5.2 存在，将数据写入redis
            json = JSONUtil.toJsonStr(r);
            set(key, json, timeout, unit);
            //7、 返回商铺信息
            return r;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            //6释放锁
            unlock(RedisConstants.LOCK_SHOP_KEY + id);
        }
    }
}
