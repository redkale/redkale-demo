/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.annotation.LogLevel;
import org.redkale.convert.*;
import org.redkale.demo.base.UserInfo;
import org.redkale.persistence.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Cacheable
@LogLevel("FINER")
@Table(comment = "用户信息表")
public class UserDetail extends UserInfo {

    private static final Reproduce<UserInfo, UserDetail> reproduce = Reproduce.create(UserInfo.class, UserDetail.class);

    public static final short REGTYPE_ACCOUNT = 10; //账号注册

    public static final short REGTYPE_MOBILE = 20; //手机注册

    public static final short REGTYPE_EMAIL = 30; //邮箱注册

    public static final short REGTYPE_WEIXIN = 40;  //微信注册

    public static final short REGTYPE_QQOPEN = 50; //QQ注册

    @Column(updatable = false, comment = "[注册类型]: 10:账号注册; 20:手机注册; 30:邮箱注册; 40:微信注册; 50:QQ注册")
    private short regtype;

    @Column(updatable = false, comment = "[创建时间]")
    private long createtime;

    @Column(updatable = false, length = 255, comment = "[注册终端]")
    private String regagent = "";//注册终端

    @Column(updatable = false, length = 64, comment = "[注册IP]")
    private String regaddr = "";//注册IP

    @Column(length = 255, comment = "[备注]")
    private String remark = ""; //备注

    @Column(comment = "[更新时间]")
    private long updatetime;  //修改时间

    public UserInfo createUserInfo() {
        return reproduce.apply(new UserInfo(), this);
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
