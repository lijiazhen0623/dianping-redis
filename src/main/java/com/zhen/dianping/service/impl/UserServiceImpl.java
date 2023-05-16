package com.zhen.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhen.dianping.dto.LoginFormDTO;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.dto.UserDTO;
import com.zhen.dianping.entity.User;
import com.zhen.dianping.mapper.UserMapper;
import com.zhen.dianping.service.IUserService;
import com.zhen.dianping.utils.RedisConstants;
import com.zhen.dianping.utils.RegexUtils;
import com.zhen.dianping.utils.SystemConstants;
import com.zhen.dianping.utils.VerificationCodeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendVerificationCode(String phone, HttpSession session) {
        if (StrUtil.isEmpty(phone)) {
            return Result.fail("邮箱为空");
        }
        //1、校验邮箱格式
        if (RegexUtils.isEmailInvalid(phone)) {
            return Result.fail("邮箱格式不正确");
        }
        //2、生成验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        //3、保存验证码到redis
//        session.setAttribute("code", verificationCode);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,verificationCode,5, TimeUnit.MINUTES);
        //4、发送验证码
        VerificationCodeUtils.sendVerificationCode(phone,verificationCode);
        return Result.ok();
    }

    @Override
    public Result loginOrRegister(LoginFormDTO loginForm, HttpSession session) {
        if (loginForm == null) {
            return Result.fail("提交信息为空");
        }
        //1、校验验证码
        String code = loginForm.getCode();
        if (StrUtil.isEmpty(code)) {
            return Result.fail("验证码为空");
        }
//        if (!code.equals(session.getAttribute("code"))) {
//            return Result.fail("验证码错误");
//        }
        String vCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());
        if (!code.equals(vCode)) {
            return Result.fail("验证码错误");
        }
        //2、根据手机号(邮箱)查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        //2.1 用户不存在
        if (user == null) {
            //2.2 创建用户
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX+RandomUtil.randomString(8));
            createUser(user);
        }
        //3、将用户保存到redis
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
//        session.setAttribute("user",userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreError(false)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY+token,userMap);
        //设置过期时间
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,30,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public void createUser(User user) {
        save(user);
    }
}
