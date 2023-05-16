package com.zhen.dianping.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.zhen.dianping.dto.UserDTO;
import com.zhen.dianping.utils.RedisConstants;
import com.zhen.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author LiJiaZhen
 * @date 2023/5/15 16:28
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1、从redis获取用户信息(前端会在请求头中添加authorization属性，值为token)
        String token = request.getHeader("authorization");
        if (StrUtil.isEmpty(token)) {
            return true;
        }
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //2、判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        //3、保存用户信息到ThreadLocal
        UserDTO userDTO = new UserDTO();
        BeanUtil.fillBeanWithMap(userMap,userDTO,false);
        UserHolder.saveUser(userDTO);
        //4、更新redis中用户数据的过期时间
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,30, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //释放ThreadLocal，防止内存泄漏
        UserHolder.removeUser();
    }
}
