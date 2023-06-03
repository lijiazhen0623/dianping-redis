package com.zhen.dianping.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;

    /**
     * 数据对象
     */
    private Object data;
}
