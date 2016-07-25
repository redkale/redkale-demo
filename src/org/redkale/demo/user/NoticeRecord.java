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
public class NoticeRecord extends BaseEntity {

    //未发送
    public static final short NOTICESTATUS_UNSEND = 10;

    //已发送
    public static final short NOTICESTATUS_SENDOK = 20;

    //发送失败
    public static final short NOTICESTATUS_SENDNO = 30;

    @Id
    @GeneratedValue
    private long noticeid; //消息ID

    @Column(updatable = false)
    private int userid; //用户ID

    private short status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    @Column(updatable = false)
    private String apptoken = "";  //设备推送ID

    @Column(updatable = false)
    private String content = ""; //短信内容

    private String resultdesc = ""; //返回结果

    @Column(updatable = false)
    private long createtime; //创建时间

    public long getNoticeid() {
        return noticeid;
    }

    public void setNoticeid(long noticeid) {
        this.noticeid = noticeid;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getApptoken() {
        return apptoken;
    }

    public void setApptoken(String apptoken) {
        this.apptoken = apptoken;
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
