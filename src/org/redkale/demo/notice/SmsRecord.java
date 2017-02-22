package org.redkale.demo.notice;

import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(catalog = "demo_notice")
@DistributeTable(strategy = SmsRecord.TableStrategy.class)
public class SmsRecord extends BaseEntity {

    //未发送
    public static final short SMSSTATUS_UNSEND = 10;

    //已发送
    public static final short SMSSTATUS_SENDOK = 20;

    //发送失败
    public static final short SMSSTATUS_SENDNO = 30;

    public static final short SMSTYPE_VERCODE = 10; //验证码短信

    public static final short SMSTYPE_MARKETS = 20; //营销短信

    //手机号码注册
    public static final short CODETYPE_REG = RandomCode.TYPE_SMSREG;

    //短信重置密码
    public static final short CODETYPE_PWD = RandomCode.TYPE_SMSPWD;

    //修改手机号码
    public static final short CODETYPE_MOB = RandomCode.TYPE_SMSMOB;

    //用户验证码登录
    public static final short CODETYPE_LGN = RandomCode.TYPE_SMSLGN;

    @Id
    @GeneratedValue
    @Column(length = 64, comment = "短信ID UUID")
    private String smsid = "";

    @Column(comment = "短信类型; 10:验证码；20:营销短信；")
    private short smstype;

    @Column(comment = "验证码类型; 10:手机注册；20:重置密码；30:修改手机；40:登录；")
    private short codetype; 

    @Column(comment = "状态; 10:未发送; 20:已发送; 30:发送失败;")
    private short status; 

    @Column(comment = "群发的短信条数")
    private int smscount;

    @Column(comment = "群发的手机号码数")
    private int mobcount = 1;

    @Column(length = 32, comment = "手机号码")
    private String mobile = ""; 

    @Column(length = 2048, comment = "群发的手机号码集合，多个用;隔开,最多100条")
    private String mobiles = ""; 

    @Column(length = 1024, comment = "短信内容")
    private String content = "";

    @Column(length = 1024, comment = "返回结果")
    private String resultdesc = ""; 

    @Column(updatable = false, comment = "生成时间，单位毫秒")
    private long createtime; 

    @Transient
    @Column(comment = "用户ID")
    private long userid;//用户ID

    public SmsRecord() {
    }

    public SmsRecord(short smstype, String mobile, String content) {
        this.codetype = smstype;
        this.status = SMSSTATUS_UNSEND;
        this.mobile = mobile;
        this.content = content;
        this.createtime = System.currentTimeMillis();
    }

    public void setSmsid(String smsid) {
        this.smsid = smsid == null ? "" : smsid;
    }

    public String getSmsid() {
        return this.smsid;
    }

    public short getSmstype() {
        return smstype;
    }

    public void setSmstype(short smstype) {
        this.smstype = smstype;
    }

    public void setCodetype(short codetype) {
        this.codetype = codetype;
    }

    public short getCodetype() {
        return this.codetype;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getStatus() {
        return this.status;
    }

    public int getSmscount() {
        return smscount;
    }

    public void setSmscount(int smscount) {
        this.smscount = smscount;
    }

    public int getMobcount() {
        return mobcount;
    }

    public void setMobcount(int mobcount) {
        this.mobcount = mobcount;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return this.mobile;
    }

    public String getMobiles() {
        return mobiles;
    }

    public void setMobiles(String mobiles) {
        this.mobiles = mobiles == null ? "" : mobiles.trim();
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return this.content;
    }

    public void setResultdesc(String resultdesc) {
        this.resultdesc = resultdesc;
    }

    public String getResultdesc() {
        return this.resultdesc;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public static class TableStrategy implements DistributeTableStrategy<SmsRecord> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, FilterNode node) {
            Range.LongRange createtime = (Range.LongRange) node.findValue("createtime");
            return getTable(table, createtime.getMin());
        }

        @Override
        public String getTable(String table, SmsRecord bean) {
            return getTable(table, bean.getCreatetime());
        }

        private String getTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return "demo_notice." + table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
