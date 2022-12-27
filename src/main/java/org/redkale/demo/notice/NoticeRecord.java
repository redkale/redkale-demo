/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import java.io.Serializable;
import org.redkale.demo.base.BaseEntity;
import org.redkale.persistence.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "消息推送表")
@DistributeTable(strategy = NoticeRecord.TableStrategy.class)
public class NoticeRecord extends BaseEntity {

    //未发送
    public static final short NOTICESTATUS_UNSEND = 10;

    //已发送
    public static final short NOTICESTATUS_SENDOK = 20;

    //发送失败
    public static final short NOTICESTATUS_SENDNO = 30;

    @Id
    @Column(length = 64, comment = "消息ID 值=create36time(9位)+UUID(32位)")
    private String noticeid; //消息ID

    @Column(updatable = false, comment = "用户ID")
    private long userid; //用户ID

    @Column(comment = "状态; 10:未发送; 20:已发送; 30:发送失败;")
    private short status; //状态; 10:未发送; 20:已发送; 30:发送失败;

    @Column(length = 16, comment = "APP的设备系统(小写); android/ios")
    private String appos = "";

    @Column(length = 128, updatable = false, comment = "设备推送ID")
    private String appToken = "";  //设备推送ID

    @Column(length = 4096, updatable = false, comment = "短信内容")
    private String content = ""; //短信内容

    @Column(length = 4096, comment = "返回结果")
    private String resultdesc = ""; //返回结果

    @Column(updatable = false, comment = "创建时间")
    private long createTime; //创建时间

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

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
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

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public static class TableStrategy implements DistributeTableStrategy<NoticeRecord> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String[] getTables(String table, FilterNode node) {
            Object time = node.findValue("createTime");
            if (time instanceof Long) {
                return new String[]{getSingleTable(table, (Long) time)};
            }
            Range.LongRange createTime = (Range.LongRange) time;
            return new String[]{getSingleTable(table, createTime.getMin())};
        }

        @Override
        public String getTable(String table, NoticeRecord bean) {
            return getSingleTable(table, bean.getCreateTime());
        }

        private String getSingleTable(String table, long createTime) {
            int pos = table.indexOf('.');
            return "redemo_notice." + table.substring(pos + 1) + "_" + String.format(format, createTime);
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getSingleTable(table, Long.parseLong(id.substring(0, 9), 36));
        }
    }
}
