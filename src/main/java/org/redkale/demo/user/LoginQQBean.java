/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.demo.base.BaseBean;

/**
 * QQ登录参数
 *
 * @author zhangjx
 */
public class LoginQQBean extends BaseBean {

    protected String accessToken;

    protected String openid;

    protected String loginAgent;

    protected String loginAddr;

    protected String sessionid;

    protected String appos = "";
    
    protected String appToken = "";

    public boolean emptyAccessToken() {
        return this.accessToken == null || this.accessToken.isEmpty();
    }

    public boolean emptyAppToken() {
        return this.appToken == null || this.appToken.isEmpty();
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        if (appToken != null && appToken.length() > 31) {
            this.appToken = appToken;
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getLoginAgent() {
        return loginAgent;
    }

    public void setLoginAgent(String loginAgent) {
        this.loginAgent = loginAgent;
    }

    public String getLoginAddr() {
        return loginAddr;
    }

    public void setLoginAddr(String loginAddr) {
        this.loginAddr = loginAddr;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        this.appos = appos;
    }

}
