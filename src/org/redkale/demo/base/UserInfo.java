/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import javax.persistence.Id;
import org.redkale.convert.*;
import org.redkale.source.VirtualEntity;
import org.redkale.util.Reproduce;

/**
 *
 * @author zhangjx
 */
/**
 * 头像的url： http://redkale.org/dir/face_xx/{userid的36进制}.jpg
 *
 * @author zhangjx
 */
@VirtualEntity(direct = true, loader = UserInfoLoader.class)
public class UserInfo extends BaseEntity {

    private static final Reproduce<UserInfo, UserInfo> reproduce = Reproduce.create(UserInfo.class, UserInfo.class);

    //男
    public static final short GENDER_MALE = 2;

    //女
    public static final short GENDER_FEMALE = 4;

    //平台的虚拟用户ID
    public static final int USERID_SYSTEM = 100000000;

    public static final UserInfo USER_SYSTEM = new UserInfo();

    static {
        USER_SYSTEM.setUserid(USERID_SYSTEM);
        USER_SYSTEM.setUsername("SYSTEM");
        USER_SYSTEM.setEmail("system@redkale.org");
        USER_SYSTEM.setMobile("");
    }

    @Id
    protected int userid;  //用户ID

    protected String username = "";  //用户昵称

    protected short type;    //用户类型 （前端不可见）

    protected String password = ""; //密码（前端不可见） 数据库存放的密码规则为: HEX-SHA1( HEX-MD5( HEX-MD5(明文)+"-REDKALE" ) +"-REDKALE" )

    protected String account = "";  //用户账号（前端不可见）

    protected String mobile = "";  //手机号码（前端不可见）

    protected String email = "";  //邮箱  （前端不可见）

    protected String wxunionid = "";  //微信openid （前端不可见）

    protected String qqopenid = "";  //QQ openid （前端不可见）

    protected String apptoken = "";  //APP的设备ID （前端不可见） 通常用于IOS的APNS推送

    protected short status;    //状态 （前端不可见）  值见BaseEntity的STATUS常量

    protected short gender; //性别; 2:男;  4:女; 值见BaseEntity的GENDER常量

    protected long infotime; //用户可见资料的更新时间 通常用于客户端判断用户资料是否已修改便于主动拉取新资料

    public UserInfo copy() {
        return reproduce.copy(new UserInfo(), this);
    }

    public UserInfo copyTo(UserInfo dest) {
        return reproduce.copy(dest, this);
    }

    public boolean checkAuth(int moduleid, int actionid) {
        if (moduleid == 0 || actionid == 0) return true;
        //权限判断
        return true;
    }

    //用户是否处于正常状态
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isNormal() {
        return this.status == STATUS_NORMAL;
    }

    //用户是否处于待审批状态
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isPending() {
        return this.status == STATUS_PENDING;
    }

    //用户是否处于禁用状态
    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isFrobid() {
        return this.status == STATUS_FREEZE;
    }

    public long getInfotime() {
        return infotime;
    }

    public void setInfotime(long infotime) {
        this.infotime = infotime;
    }

    public int getUserid() {
        return userid;
    }

    public String getUser36id() {
        return Integer.toString(userid, 36);
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    //用户帐号不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getAccount() {
        return account == null ? "" : account;
    }

    public void setAccount(String account) {
        this.account = account == null ? "" : account.trim();
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username.trim();
    }

    //密码不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password.trim();
    }

    //手机号码不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMobile() {
        return mobile == null ? "" : mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile == null ? "" : mobile.trim();
    }

    //邮箱地址不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    //微信绑定ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getWxunionid() {
        return wxunionid == null ? "" : wxunionid;
    }

    public void setWxunionid(String wxunionid) {
        this.wxunionid = wxunionid == null ? "" : wxunionid.trim();
    }

    //QQ绑定ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getQqopenid() {
        return qqopenid == null ? "" : qqopenid;
    }

    public void setQqopenid(String qqopenid) {
        this.qqopenid = qqopenid == null ? "" : qqopenid.trim();
    }

    //APP设备ID不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getApptoken() {
        return apptoken == null ? "" : apptoken;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken == null ? "" : apptoken.trim();
    }

    //用户状态值不允许输出给外部接口
    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getGender() {
        return gender;
    }

    public void setGender(short gender) {
        this.gender = gender;
    }

}
