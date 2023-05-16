package com.zhen.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhen.dianping.dto.LoginFormDTO;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendVerificationCode(String phone, HttpSession session);

    Result loginOrRegister(LoginFormDTO loginForm, HttpSession session);

    void createUser(User user);
}
