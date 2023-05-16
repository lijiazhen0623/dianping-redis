package com.zhen.dianping.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.HtmlEmail;

/**
 * @author LiJiaZhen
 * @date 2023/5/15 14:44
 */
@Slf4j
public class VerificationCodeUtils {
    /**
     * 发送邮箱验证码
     * @param toEmail 被发送者
     * @param verificationCode 验证码
     */
    public static void sendVerificationCode(String toEmail,String verificationCode){
        HtmlEmail htmlEmail = new HtmlEmail();
        try {
            htmlEmail.setHostName("smtp.qq.com");
            //第一个参数是发送者的QQEamil邮箱   第二个参数是刚刚获取的授权码
            htmlEmail.setAuthentication("1722249048@qq.com", "xhyfiorpmxupbecc");
            //发送人的邮箱为自己的，用户名可以随便填  记得是自己的邮箱不是qq
            htmlEmail.setFrom("1722249048@qq.com", "");
//			send.setSmtpPort(465); 	//端口号 可以不开
            //开启SSL加密
            htmlEmail.setSSLOnConnect(true);
            htmlEmail.setCharset("utf-8");
            //设置收件人    email为你要发送给谁的邮箱账户
            htmlEmail.addTo(toEmail);
            //邮箱标题
            htmlEmail.setSubject("验证码");
            //Eamil发送的内容
            htmlEmail.setMsg("<font color='red'>您的验证码:</font>   " + verificationCode + " ，五分钟后失效");
            //发送
            htmlEmail.send();
        }catch (Exception e){
            log.error("验证码发送失败：{}",e.getMessage());
        }
    }
}
