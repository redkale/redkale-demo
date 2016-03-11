/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import javax.persistence.*;
import org.redkale.demo.base.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public final class LoginBean extends BaseBean implements FilterBean {

    private String account;  //登录账号: 用户名、邮箱或者手机号码(为便于区别，用户名规则：不能以数字开头或者包含@)

    private String password; //明文密码一次MD5值

    private String apptoken = ""; //APP设备唯一标识

    private String cookieinfo; //自动登录Cookie值

    private String loginagent = ""; //User-Agent

    private String loginip = ""; //客户端IP地址

    private String vercode = ""; //验证码

    private int cacheday; //COOKIE缓存天数

    @Transient
    private String sessionid = ""; // session ID

    public boolean emptyAccount() {
        return this.account == null || this.account.isEmpty();
    }

    public boolean emptyPassword() {
        return this.password == null || this.password.isEmpty();
    }

    public boolean emptyApptoken() {
        return this.apptoken == null || this.apptoken.isEmpty();
    }

    public boolean emptySessionid() {
        return this.sessionid == null || this.sessionid.isEmpty();
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

    public String getLoginagent() {
        return loginagent;
    }

    public void setLoginagent(String loginagent) {
        if (loginagent != null) {
            if (loginagent.length() > 128) loginagent = loginagent.substring(0, 127);
            this.loginagent = loginagent;
        }
    }

    public String getLoginip() {
        return loginip;
    }

    public void setLoginip(String loginip) {
        this.loginip = loginip;
    }

    public String getVercode() {
        return vercode;
    }

    public void setVercode(String vercode) {
        this.vercode = vercode;
    }

    public int getCacheday() {
        return cacheday;
    }

    public void setCacheday(int cacheday) {
        this.cacheday = cacheday;
    }

    public String getCookieinfo() {
        return cookieinfo;
    }

    public void setCookieinfo(String cookieinfo) {
        this.cookieinfo = cookieinfo;
    }

    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        if (apptoken != null && apptoken.length() > 31) {
            this.apptoken = apptoken;
        }
    }

}
