/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.security.*;
import javax.persistence.*;
import org.redkale.convert.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
/**
 * 头像的url： http://xxx/user/{userid的36进制}
 *
 * @author zhangjx
 */
@VirtualEntity
public class UserInfo extends BaseEntity {

    private static final Reproduce<UserInfo, UserInfo> reproduce = Reproduce.create(UserInfo.class, UserInfo.class);

    //男
    public static final short GENDER_MALE = 2;

    //女
    public static final short GENDER_FEMALE = 4;

    protected static final MessageDigest sha1;

    static {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        sha1 = d;
    }

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

    protected String username = "";  //用户名

    protected String password = ""; //密码（前端不可见）

    protected String mobile = "";  //手机号码（前端不可见）

    protected String email = "";  //邮箱  （前端不可见）

    protected long infotime; //用户可见资料的更新时间

    protected String wxunionid = "";  //微信openid （前端不可见）

    protected String qqopenid = "";  //QQ openid （前端不可见）

    protected String apptoken = "";  //APP的设备ID （前端不可见）

    protected short status;    //状态 （前端不可见）

    protected short gender; //性别; 2:男;  4:女;

    @Override
    public int hashCode() {
        return (int) this.userid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        return this.userid == ((UserInfo) obj).userid;
    }

    public UserInfo copy() {
        return reproduce.copy(new UserInfo(), this);
    }

    public UserInfo copyTo(UserInfo dest) {
        return reproduce.copy(dest, this);
    }

    /**
     * 校验密码是否正确
     *
     * @param passwordmd5
     * @return
     */
    public boolean checkPassword(String passwordmd5) {
        if (this.password.isEmpty() && passwordmd5.isEmpty()) return true;
        byte[] bytes = ("REDKALE-" + password.trim()).getBytes();
        synchronized (sha1) {
            bytes = sha1.digest(bytes);
        }
        return this.password.equals(new String(Utility.binToHex(bytes)));
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isNormal() {
        return this.status == STATUS_NORMAL;
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isPending() {
        return this.status == STATUS_PENDING;
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isFrobid() {
        return this.status == STATUS_FREEZE;
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isMale() {
        return this.gender == GENDER_MALE;
    }

    @ConvertColumn(ignore = true, type = ConvertType.ALL)
    public boolean isHasapptoken() {
        return this.apptoken != null && !this.apptoken.isEmpty();
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isEm() {
        return this.email != null && !this.email.isEmpty();
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isMb() {
        return this.mobile != null && !this.mobile.isEmpty();
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isWx() {
        return this.wxunionid != null && !this.wxunionid.isEmpty();
    }

    @ConvertColumn(ignore = true, type = ConvertType.BSON)
    public boolean isQq() {
        return this.qqopenid != null && !this.qqopenid.isEmpty();
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        if (mobile != null) this.mobile = mobile.trim();
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        if (email != null) this.email = email.trim().toLowerCase();
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getWxunionid() {
        return wxunionid;
    }

    public void setWxunionid(String wxunionid) {
        this.wxunionid = wxunionid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getQqopenid() {
        return qqopenid;
    }

    public void setQqopenid(String qqopenid) {
        this.qqopenid = qqopenid;
    }

    @ConvertColumn(ignore = true, type = ConvertType.JSON)
    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken;
    }

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
