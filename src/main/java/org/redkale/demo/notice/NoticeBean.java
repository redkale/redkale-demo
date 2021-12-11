/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import javax.persistence.Column;
import org.redkale.demo.base.BaseBean;
import org.redkale.source.FilterBean;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Comment("消息推送过滤类")
public class NoticeBean extends BaseBean implements FilterBean {

    @Column(comment = "用户ID")
    private long userid;

    @Column(comment = "状态; 10:未发送; 20:已发送; 30:发送失败;")
    private short[] status;

    @Column(length = 16, comment = "APP的设备系统(小写); android/ios")
    private String appos = "";

    @Column(length = 128, updatable = false, comment = "设备推送ID")
    private String apptoken = "";  //设备推送ID

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public short[] getStatus() {
        return status;
    }

    public void setStatus(short[] status) {
        this.status = status;
    }

    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken;
    }

    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        this.appos = appos;
    }

}
