/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import org.redkale.annotation.Comment;
import org.redkale.demo.base.BaseBean;
import org.redkale.persistence.Column;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class SmsBean extends BaseBean implements FilterBean {

    @Column(comment = "状态; 10:未发送; 20:已发送; 30:发送失败;")
    private short[] status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    @Column(comment = "短信类型; 10:验证码；20:营销短信；")
    private short[] smstype; //发送类型

    @Column(length = 32, comment = "手机号码")
    private String mobile;  //手机号码

    @Comment("时间范围，本字段必须有值，且范围必须在一个自然月内")
    private Range.LongRange createTime; //时间范围

    public short[] getStatus() {
        return status;
    }

    public void setStatus(short[] status) {
        this.status = status;
    }

    public short[] getSmstype() {
        return smstype;
    }

    public void setSmstype(short[] smstype) {
        this.smstype = smstype;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Range.LongRange getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Range.LongRange createTime) {
        this.createTime = createTime;
    }

}
