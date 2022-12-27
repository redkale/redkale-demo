package org.redkale.demo.pay;

import java.io.Serializable;
import org.redkale.demo.base.BaseEntity;
import org.redkale.persistence.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "支付接口结果表")
@DistributeTable(strategy = PayAction.TableStrategy.class)
public class PayAction extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID 值=create36time(9位)+UUID(32位)")
    private String payactid = "";

    @Column(length = 128, comment = "支付编号")
    private String payno = "";

    @Column(comment = "支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;")
    private short paytype = 10;

    @Column(length = 1024, comment = "请求的URL")
    private String acturl = "";

    @Column(length = 2048, comment = "支付接口请求对象")
    private String requestjson = "";

    @Column(length = 5120, comment = "支付接口返回的原始结果")
    private String responsetext = "";

    @Column(updatable = false, comment = "创建时间，单位毫秒")
    private long createTime;

    public void setPayactid(String payactid) {
        this.payactid = payactid;
    }

    public String getPayactid() {
        return this.payactid;
    }

    public void setPayno(String payno) {
        this.payno = payno;
    }

    public String getPayno() {
        return this.payno;
    }

    public void setPaytype(short paytype) {
        this.paytype = paytype;
    }

    public short getPaytype() {
        return this.paytype;
    }

    public void setActurl(String acturl) {
        this.acturl = acturl;
    }

    public String getActurl() {
        return this.acturl;
    }

    public void setRequestjson(String requestjson) {
        this.requestjson = requestjson;
    }

    public String getRequestjson() {
        return this.requestjson;
    }

    public void setResponsetext(String responsetext) {
        this.responsetext = responsetext;
    }

    public String getResponsetext() {
        return this.responsetext;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public static class TableStrategy implements DistributeTableStrategy<PayAction> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, PayAction bean) {
            return getSingleTable(table, bean.getCreateTime());
        }

        private String getSingleTable(String table, long createTime) {
            int pos = table.indexOf('.');
            return "redemo_pay_act." + table.substring(pos + 1) + "_" + String.format(format, createTime);
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getTable(table, Long.parseLong(id.substring(0, 9), 36));
        }

        @Override
        public String[] getTables(String table, FilterNode node) {
            Object time = node.findValue("createTime");
            if (time instanceof Long) {
                return new String[]{getSingleTable(table, (Long) time)};
            }
            Range.LongRange createTime = (Range.LongRange) time;
            return new String[]{getSingleTable(table, createTime.getMin())};
        }

    }
}
