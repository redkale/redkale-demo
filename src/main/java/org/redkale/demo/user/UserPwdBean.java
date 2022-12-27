/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.annotation.Comment;
import org.redkale.demo.base.BaseBean;

/**
 *
 * @author zhangjx
 */
public class UserPwdBean extends BaseBean {

    @Comment("Session会话ID")
    private String sessionid;

    @Comment("随机码")
    private String randomcode = "";

    @Comment("用户账号")
    private String account = "";

    @Comment("验证码")
    private String verCode = "";

    @Comment("旧密码 MD5(密码明文)")
    private String oldpwd;  //HEX-MD5(密码明文)

    @Comment("新密码 MD5(密码明文)")
    private String newpwd;  //HEX-MD5(密码明文)

    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    public String getOldpwd() {
        return oldpwd;
    }

    public void setOldpwd(String oldpwd) {
        this.oldpwd = oldpwd;
    }

    public String getNewpwd() {
        return newpwd;
    }

    public void setNewpwd(String newpwd) {
        this.newpwd = newpwd;
    }

    public String getRandomcode() {
        return randomcode;
    }

    public void setRandomcode(String randomcode) {
        this.randomcode = randomcode;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getVerCode() {
        return verCode;
    }

    public void setVerCode(String verCode) {
        this.verCode = verCode;
    }

}
