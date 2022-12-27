/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.annotation.Comment;
import org.redkale.demo.base.BaseBean;
import org.redkale.persistence.Transient;
import org.redkale.source.FilterBean;

/**
 *
 * @author zhangjx
 */
public final class LoginBean extends BaseBean implements FilterBean {

    @Comment("登录账号")
    private String account;  //登录账号: 用户名、邮箱或者手机号码(为便于区别，用户名规则：不能以数字开头或者包含@)

    @Comment("用户类型")
    private short type; //用户类型

    @Comment("MD5(密码明文)")
    private String password = ""; //HEX-MD5(HEX-MD5(密码明文))

    @Comment("APP的设备系统(小写); android/ios/web/wap")
    private String appos = "";//APP的设备系统

    @Comment("APP设备唯一标识")
    private String appToken = ""; //APP设备唯一标识

    @Comment("自动登录Cookie值")
    private String cookieInfo; //自动登录Cookie值

    @Comment("User-Agent")
    private String loginAgent = ""; //User-Agent

    @Comment("客户端IP地址")
    private String loginIp = ""; //客户端IP地址

    @Comment("验证码")
    private String verCode = ""; //验证码

    @Comment("COOKIE缓存天数")
    private int cacheDay; //COOKIE缓存天数

    @Transient
    @Comment("Session会话ID")
    private String sessionid = ""; // session ID

    public boolean emptyAccount() {
        return this.account == null || this.account.isEmpty();
    }

    public boolean emptyPassword() {
        return this.password == null || this.password.isEmpty();
    }

    public boolean emptyAppToken() {
        return this.appToken == null || this.appToken.isEmpty();
    }

    public boolean emptySessionid() {
        return this.sessionid == null || this.sessionid.isEmpty();
    }

    public boolean emptyCookieInfo() {
        return this.cookieInfo == null || this.cookieInfo.isEmpty();
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginAgent() {
        return loginAgent;
    }

    public void setLoginAgent(String loginAgent) {
        if (loginAgent != null) {
            if (loginAgent.length() > 128) loginAgent = loginAgent.substring(0, 127);
            this.loginAgent = loginAgent;
        }
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public String getVerCode() {
        return verCode;
    }

    public void setVerCode(String verCode) {
        this.verCode = verCode;
    }

    public int getCacheDay() {
        return cacheDay;
    }

    public void setCacheDay(int cacheDay) {
        this.cacheDay = cacheDay;
    }

    public String getCookieInfo() {
        return cookieInfo;
    }

    public void setCookieInfo(String cookieInfo) {
        this.cookieInfo = cookieInfo;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        if (appToken != null) {
            this.appToken = appToken.trim();
        }
    }

    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        if (appos != null) {
            this.appos = appos.trim().toLowerCase();
        }
    }

}
