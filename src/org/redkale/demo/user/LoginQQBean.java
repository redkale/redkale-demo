/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.demo.base.*;

/**
 * QQ登录参数
 *
 * @author zhangjx
 */
public class LoginQQBean extends BaseBean {

    protected String accesstoken;

    protected String openid;

    protected String loginagent;

    protected String loginaddr;

    protected String sessionid;

    protected String appos = "";
    
    protected String apptoken = "";

    public boolean emptyAccesstoken() {
        return this.accesstoken == null || this.accesstoken.isEmpty();
    }

    public boolean emptyApptoken() {
        return this.apptoken == null || this.apptoken.isEmpty();
    }

    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        if (apptoken != null && apptoken.length() > 31) {
            this.apptoken = apptoken;
        }
    }

    public String getAccesstoken() {
        return accesstoken;
    }

    public void setAccesstoken(String accesstoken) {
        this.accesstoken = accesstoken;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getLoginagent() {
        return loginagent;
    }

    public void setLoginagent(String loginagent) {
        this.loginagent = loginagent;
    }

    public String getLoginaddr() {
        return loginaddr;
    }

    public void setLoginaddr(String loginaddr) {
        this.loginaddr = loginaddr;
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
