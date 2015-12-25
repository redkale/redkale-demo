/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.demo.base.*;

/**
 *
 * @author zhangjx
 */
public class QQLoginBean extends BaseBean {

    protected String accesstoken;

    protected String openid;

    protected String reghost;

    protected String regaddr;

    protected String sessionid;

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

    public String getReghost() {
        return reghost;
    }

    public void setReghost(String reghost) {
        this.reghost = reghost;
    }

    public String getRegaddr() {
        return regaddr;
    }

    public void setRegaddr(String regaddr) {
        this.regaddr = regaddr;
    }

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

}
