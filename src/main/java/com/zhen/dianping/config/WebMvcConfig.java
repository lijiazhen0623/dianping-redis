package com.zhen.dianping.config;

import com.zhen.dianping.interceptor.LoginInterceptor;
import com.zhen.dianping.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author LiJiaZhen
 * @date 2023/5/15 16:37
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 注册拦截器
     * @param registry 注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //用户登录校验
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        ).order(1);
        //刷新token的过期时间
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
    }
}
