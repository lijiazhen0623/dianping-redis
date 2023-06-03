package com.zhen.dianping.utils;

/**
 * @author LiJiaZhen
 * @date 2023/5/27 15:40
 */
public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁的超时过期时间，过期后自动释放
     * @return true 获取成功 false 获取失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
