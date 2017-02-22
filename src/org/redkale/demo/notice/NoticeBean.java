/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import org.redkale.demo.base.BaseBean;
import org.redkale.source.FilterBean;



/**
 *
 * @author zhangjx
 */
public class NoticeBean extends BaseBean implements FilterBean {

    private int userid; //用户ID

    private short[] status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    private String apptoken = "";  //设备推送ID

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
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

}
