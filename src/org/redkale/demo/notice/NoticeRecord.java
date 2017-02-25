/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(catalog = "redemo_notice", comment = "消息推送表")
@DistributeTable(strategy = NoticeRecord.TableStrategy.class)
public class NoticeRecord extends BaseEntity {

    //未发送
    public static final short NOTICESTATUS_UNSEND = 10;

    //已发送
    public static final short NOTICESTATUS_SENDOK = 20;

    //发送失败
    public static final short NOTICESTATUS_SENDNO = 30;

    @Id
    @GeneratedValue
    @Column(comment = "UUID")
    private String noticeid; //消息ID

    @Column(updatable = false, comment = "用户ID")
    private long userid; //用户ID

    @Column(comment = "状态; 10:未发送; 20:已发送; 30:发送失败;")
    private short status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    @Column(length = 16, comment = "APP的设备系统(小写); android/ios")
    private String appos = "";

    @Column(updatable = false, comment = "设备推送ID")
    private String apptoken = "";  //设备推送ID

    @Column(updatable = false, comment = "短信内容")
    private String content = ""; //短信内容

    @Column(comment = "返回结果")
    private String resultdesc = ""; //返回结果

    @Column(updatable = false, comment = "创建时间")
    private long createtime; //创建时间

    public String getNoticeid() {
        return noticeid;
    }

    public void setNoticeid(String noticeid) {
        this.noticeid = noticeid;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getAppos() {
        return appos;
    }

    public void setAppos(String appos) {
        this.appos = appos;
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

    public static class TableStrategy implements DistributeTableStrategy<NoticeRecord> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, FilterNode node) {
            Range.LongRange createtime = (Range.LongRange) node.findValue("createtime");
            return getTable(table, createtime.getMin());
        }

        @Override
        public String getTable(String table, NoticeRecord bean) {
            return getTable(table, bean.getCreatetime());
        }

        private String getTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return "redemo_notice." + table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
