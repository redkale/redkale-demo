/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
public class SmsRecord extends BaseEntity {

    //未发送
    public static final short SMSSTATUS_UNSEND = 10;

    //已发送
    public static final short SMSSTATUS_SENDOK = 20;

    //发送失败
    public static final short SMSSTATUS_SENDNO = 30;

    //手机号码注册
    public static final short SMSTYPE_REG = RandomCode.TYPE_SMSREG;

    //短信重置密码
    public static final short SMSTYPE_PWD = RandomCode.TYPE_SMSPWD;

    //修改手机号码
    public static final short SMSTYPE_MOB = RandomCode.TYPE_SMSMOB;

    //用户验证码登录
    public static final short SMSTYPE_LGN = RandomCode.TYPE_SMSLGN;

    @Id
    @GeneratedValue
    private long smsid; //自增长ID

    private short smstype; //发送类型

    private short status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    private String mobile = "";  //手机号码

    private String content = ""; //短信内容

    private String resultdesc = ""; //返回结果

    private long createtime; //创建时间

    public SmsRecord() {
    }

    public SmsRecord(short smstype, String mobile, String content) {
        this.smstype = smstype;
        this.status = SMSSTATUS_UNSEND;
        this.mobile = mobile;
        this.content = content;
        this.createtime = System.currentTimeMillis();
    }

    public long getSmsid() {
        return smsid;
    }

    public void setSmsid(long smsid) {
        this.smsid = smsid;
    }

    public short getSmstype() {
        return smstype;
    }

    public void setSmstype(short smstype) {
        this.smstype = smstype;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getResultdesc() {
        return resultdesc;
    }

    public void setResultdesc(String resultdesc) {
        this.resultdesc = resultdesc;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

}
