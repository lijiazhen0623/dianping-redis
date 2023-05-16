package com.zhen.dianping.interceptor;

import com.zhen.dianping.dto.UserDTO;
import com.zhen.dianping.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author LiJiaZhen
 * @date 2023/5/15 16:28
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断用户是否存在
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
