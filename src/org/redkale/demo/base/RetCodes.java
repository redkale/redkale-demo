/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import org.redkale.service.RetResult;

/**
 *
 * @author zhangjx
 */
public abstract class RetCodes {

    private RetCodes() {
    }

    //------------------------------------- 用户模块 -----------------------------------------
    @RetInfo("未登陆")
    public static final int RET_USER_UNLOGIN = 1010001;

    @RetInfo("用户登录失败")
    public static final int RET_USER_LOGIN_FAIL = 1010002;

    @RetInfo("用户或密码错误")
    public static final int RET_USER_ACCOUNT_PWD_ILLEGAL = 1010003;

    @RetInfo("密码设置无效")
    public static final int RET_USER_PASSWORD_ILLEGAL = 1010004;

    @RetInfo("用户被禁用")
    public static final int RET_USER_FREEZED = 1010005;

    @RetInfo("用户不存在")
    public static final int RET_USER_NOTEXISTS = 1010008;

    @RetInfo("用户状态异常")
    public static final int RET_USER_STATUS_ILLEGAL = 1010009;

    @RetInfo("用户注册参数无效")
    public static final int RET_USER_SIGNUP_ILLEGAL = 1010011;

    @RetInfo("用户性别参数无效")
    public static final int RET_USER_GENDER_ILLEGAL = 1010021;

    @RetInfo("验证码无效")
    public static final int RET_USER_RANDCODE_ILLEGAL = 1010101; //邮件或者短信验证码

    @RetInfo("验证码已过期")
    public static final int RET_USER_RANDCODE_EXPIRED = 1010102; //邮件或者短信验证码

    @RetInfo("验证码错误或失效")
    public static final int RET_USER_CAPTCHA_ILLEGAL = 1010171; //图片验证码

    @RetInfo("用户名无效")
    public static final int RET_USER_USERNAME_ILLEGAL = 1010201;

    @RetInfo("用户账号无效")
    public static final int RET_USER_ACCOUNT_ILLEGAL = 1010301;

    @RetInfo("用户账号已存在")
    public static final int RET_USER_ACCOUNT_EXISTS = 1010302;

    @RetInfo("手机号码无效")
    public static final int RET_USER_MOBILE_ILLEGAL = 1010401;

    @RetInfo("手机号码已存在")
    public static final int RET_USER_MOBILE_EXISTS = 1010402;

    @RetInfo("手机验证码发送过于频繁")
    public static final int RET_USER_MOBILE_SMSFREQUENT = 1010406;

    @RetInfo("邮箱地址无效")
    public static final int RET_USER_EMAIL_ILLEGAL = 1010501;

    @RetInfo("邮箱地址已存在")
    public static final int RET_USER_EMAIL_EXISTS = 1010502;

    @RetInfo("微信绑定号无效")
    public static final int RET_USER_WXID_ILLEGAL = 1010601;

    @RetInfo("微信绑定号已存在")
    public static final int RET_USER_WXID_EXISTS = 1010602;

    @RetInfo("绑定微信号失败")
    public static final int RET_USER_WXID_BIND_FAIL = 1010603;

    @RetInfo("QQ绑定号无效")
    public static final int RET_USER_QQID_ILLEGAL = 1010701;

    @RetInfo("QQ绑定号已存在")
    public static final int RET_USER_QQID_EXISTS = 1010702;

    @RetInfo("绑定QQ号失败")
    public static final int RET_USER_QQID_BIND_FAIL = 1010703;

    @RetInfo("获取绑定QQ信息失败")
    public static final int RET_USER_QQID_INFO_FAIL = 1010704;

    //----------------------------------------------------------------------------------------------------
    private static final Map<Integer, String> rets = new HashMap<>();

    static {
        for (Field field : RetCodes.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() != int.class) continue;
            RetInfo info = field.getAnnotation(RetInfo.class);
            if (info == null) continue;
            int value;
            try {
                value = field.getInt(null);
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }
            rets.put(value, info.value());
        }

    }

    public static RetResult create(int retcode) {
        if (retcode == 0) return RetResult.SUCCESS;
        return new RetResult(retcode, getRetInfo(retcode));
    }

    public static String getRetInfo(int retcode) {
        if (retcode == 0) return "成功";
        return rets.getOrDefault(retcode, "未知错误");
    }

    @Target(value = {ElementType.FIELD})
    @Retention(value = RetentionPolicy.RUNTIME)
    protected @interface RetInfo {

        String value();
    }
}
