/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.lang.reflect.*;
import java.util.*;
import org.redkale.service.*;

/**
 *
 * @author zhangjx
 */
public abstract class RetCodes {

    private RetCodes() {
    }

    // 20000001 - 29999999 预留给 Redkale的扩展包使用
    //------------------------------------- 通用模块 -----------------------------------------
    @RetLabel("参数无效")
    public static final int RET_PARAMS_ILLEGAL = 30010001;

    @RetLabel("无上传文件")
    public static final int RET_UPLOAD_NOFILE = 30010101;

    @RetLabel("上传文件过大")
    public static final int RET_UPLOAD_FILETOOBIG = 30010102;

    @RetLabel("上传文件不是图片")
    public static final int RET_UPLOAD_NOTIMAGE = 30010103;

    //------------------------------------- 用户模块 -----------------------------------------
    @RetLabel("未登陆")
    public static final int RET_USER_UNLOGIN = 40010001;

    @RetLabel("用户登录失败")
    public static final int RET_USER_LOGIN_FAIL = 40010002;

    @RetLabel("用户或密码错误")
    public static final int RET_USER_ACCOUNT_PWD_ILLEGAL = 40010003;

    @RetLabel("密码设置无效")
    public static final int RET_USER_PASSWORD_ILLEGAL = 40010004;

    @RetLabel("用户被禁用")
    public static final int RET_USER_FREEZED = 40010005;

    @RetLabel("用户权限不够")
    public static final int RET_USER_AUTH_ILLEGAL = 40010006;

    @RetLabel("用户不存在")
    public static final int RET_USER_NOTEXISTS = 40010008;

    @RetLabel("用户状态异常")
    public static final int RET_USER_STATUS_ILLEGAL = 40010009;

    @RetLabel("用户注册参数无效")
    public static final int RET_USER_SIGNUP_ILLEGAL = 40010011;

    @RetLabel("用户性别参数无效")
    public static final int RET_USER_GENDER_ILLEGAL = 40010021;

    @RetLabel("验证码无效")
    public static final int RET_USER_RANDCODE_ILLEGAL = 400300101; //邮件或者短信验证码

    @RetLabel("验证码已过期")
    public static final int RET_USER_RANDCODE_EXPIRED = 400300102; //邮件或者短信验证码

    @RetLabel("验证码错误或失效")
    public static final int RET_USER_CAPTCHA_ILLEGAL = 40010171; //图片验证码

    @RetLabel("用户名无效")
    public static final int RET_USER_USERNAME_ILLEGAL = 40010201;

    @RetLabel("用户账号无效")
    public static final int RET_USER_ACCOUNT_ILLEGAL = 40010301;

    @RetLabel("用户账号已存在")
    public static final int RET_USER_ACCOUNT_EXISTS = 40010302;

    @RetLabel("手机号码无效")
    public static final int RET_USER_MOBILE_ILLEGAL = 40010401;

    @RetLabel("手机号码已存在")
    public static final int RET_USER_MOBILE_EXISTS = 40010402;

    @RetLabel("手机验证码发送过于频繁")
    public static final int RET_USER_MOBILE_SMSFREQUENT = 40010406;

    @RetLabel("邮箱地址无效")
    public static final int RET_USER_EMAIL_ILLEGAL = 40010501;

    @RetLabel("邮箱地址已存在")
    public static final int RET_USER_EMAIL_EXISTS = 40010502;

    @RetLabel("微信绑定号无效")
    public static final int RET_USER_WXID_ILLEGAL = 40010601;

    @RetLabel("微信绑定号已存在")
    public static final int RET_USER_WXID_EXISTS = 40010602;

    @RetLabel("绑定微信号失败")
    public static final int RET_USER_WXID_BIND_FAIL = 40010603;

    @RetLabel("QQ绑定号无效")
    public static final int RET_USER_QQID_ILLEGAL = 40010701;

    @RetLabel("QQ绑定号已存在")
    public static final int RET_USER_QQID_EXISTS = 40010702;

    @RetLabel("绑定QQ号失败")
    public static final int RET_USER_QQID_BIND_FAIL = 40010703;

    @RetLabel("获取绑定QQ信息失败")
    public static final int RET_USER_QQID_INFO_FAIL = 40010704;

    //----------------------------------------------------------------------------------------------------
    private static final Map<Integer, String> rets = new HashMap<>();

    static {
        for (Field field : RetCodes.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() != int.class) continue;
            RetLabel info = field.getAnnotation(RetLabel.class);
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

    public static RetResult retResult(int retcode) {
        if (retcode == 0) return RetResult.SUCCESS;
        return new RetResult(retcode, retInfo(retcode));
    }

    public static String retInfo(int retcode) {
        if (retcode == 0) return "成功";
        return rets.getOrDefault(retcode, "未知错误");
    }

}
