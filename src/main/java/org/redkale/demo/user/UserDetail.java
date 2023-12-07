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
import org.redkale.util.Copier;

/**
 *
 * @author zhangjx
 */
@Entity(cacheable = true)
@LogLevel("FINER")
@Table(comment = "用户信息表")
public class UserDetail extends UserInfo {

    private static final Copier<UserDetail, UserInfo> copier = Copier.create(UserDetail.class, UserInfo.class);

    public static final short REGTYPE_ACCOUNT = 10; //账号注册

    public static final short REGTYPE_MOBILE = 20; //手机注册

    public static final short REGTYPE_EMAIL = 30; //邮箱注册

    public static final short REGTYPE_WEIXIN = 40;  //微信注册

    public static final short REGTYPE_QQOPEN = 50; //QQ注册

    @Column(updatable = false, comment = "[注册类型]: 10:账号注册; 20:手机注册; 30:邮箱注册; 40:微信注册; 50:QQ注册")
    private short regType;

    @Column(updatable = false, comment = "[创建时间]")
    private long createTime;

    @Column(updatable = false, length = 255, comment = "[注册终端]")
    private String regAgent = "";//注册终端

    @Column(updatable = false, length = 64, comment = "[注册IP]")
    private String regAddr = "";//注册IP

    @Column(length = 255, comment = "[备注]")
    private String remark = ""; //备注

    @Column(comment = "[更新时间]")
    private long updateTime;  //修改时间

    public UserInfo createUserInfo() {
        return copier.apply(this, new UserInfo());
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
    public short getRegType() {
        return regType;
    }

    public void setRegType(short regType) {
        this.regType = regType;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegAgent() {
        return regAgent;
    }

    public void setRegAgent(String regAgent) {
        this.regAgent = regAgent;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRegAddr() {
        return regAddr;
    }

    public void setRegAddr(String regAddr) {
        this.regAddr = regAddr;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
