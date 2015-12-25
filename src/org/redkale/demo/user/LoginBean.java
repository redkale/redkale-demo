/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.demo.base.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public final class LoginBean extends BaseBean implements FilterBean {

    private String account;

    private String password;

    private String apptoken = "";

    private String cookieinfo;

    private String loginagent = "";

    private String loginip = "";

    private String vercode = "";

    private int cacheday;

    @Ignore
    private String sessionid = "";

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
