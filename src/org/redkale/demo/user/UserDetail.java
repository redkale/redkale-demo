/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.security.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.demo.base.UserInfo;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@AutoLoad
@Cacheable
@LogLevel("FINER")
public class UserDetail extends UserInfo {

    private static final Reproduce<UserInfo, UserDetail> reproduce = Reproduce.create(UserInfo.class, UserDetail.class);

    public static final short REGTYPE_ACCOUNT = 10; //账号注册

    public static final short REGTYPE_MOBILE = 20; //手机注册

    public static final short REGTYPE_EMAIL = 30; //邮箱注册

    public static final short REGTYPE_WEIXIN = 40;  //微信注册

    public static final short REGTYPE_QQOPEN = 50; //QQ注册

    @Column(updatable = false)
    private short regtype;  //注册类型

    @Column(updatable = false)
    private long createtime; //注册时间

    @Column(updatable = false)
    private String regagent = "";//注册终端

    @Column(updatable = false)
    private String regaddr = "";//注册IP

    private String remark = ""; //备注

    private long updatetime;  //修改时间

    protected static final MessageDigest sha1;

    protected static final MessageDigest md5;

    static {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        sha1 = d;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        md5 = d;
    }

    //第二次MD5
    public static String secondPasswordMD5(String passwordoncemd5) {
        byte[] bytes = ("REDKALE-" + passwordoncemd5.trim().toLowerCase()).getBytes();
        synchronized (md5) {
            bytes = md5.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    //第三次密码加密
    public static String digestPassword(String passwordtwicemd5) {
        if (passwordtwicemd5 == null || passwordtwicemd5.isEmpty()) return passwordtwicemd5;
        byte[] bytes = (passwordtwicemd5.trim().toLowerCase() + "-REDKALE").getBytes();
        synchronized (sha1) {
            bytes = sha1.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    @Override
    public int hashCode() {
        return this.userid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        return (this.userid != ((UserDetail) obj).userid);
    }

    public UserInfo createUserInfo() {
        return reproduce.copy(new UserInfo(), this);
    }

    @Override
    public String getMobile() {
        return mobile;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getWxunionid() {
        return wxunionid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getRegtype() {
        return regtype;
    }

    public void setRegtype(short regtype) {
        this.regtype = regtype;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegagent() {
        return regagent;
    }

    public void setRegagent(String regagent) {
        this.regagent = regagent;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegaddr() {
        return regaddr;
    }

    public void setRegaddr(String regaddr) {
        this.regaddr = regaddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
