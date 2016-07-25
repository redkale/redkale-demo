/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.demo.base.BaseBean;
import org.redkale.source.FilterBean;

/**
 *
 * @author zhangjx
 */
public class SmsBean extends BaseBean implements FilterBean {

    private short[] status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    private short[] smstype; //发送类型

    private String mobile;  //手机号码

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

}
