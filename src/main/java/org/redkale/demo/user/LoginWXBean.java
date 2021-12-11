/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

/**
 * 微信登录参数
 *
 * @author zhangjx
 */
public class LoginWXBean extends LoginQQBean {

    private String appid;

    private String code;

    private boolean autoreg;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isAutoreg() {
        return autoreg;
    }

    public void setAutoreg(boolean autoreg) {
        this.autoreg = autoreg;
    }

}
