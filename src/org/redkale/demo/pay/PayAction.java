package org.redkale.demo.pay;

import java.io.Serializable;
import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(catalog = "redemo_pay", comment = "支付接口结果表")
@DistributeTable(strategy = PayAction.TableStrategy.class)
public class PayAction extends BaseEntity {

    @Id
    @Column(length = 64, comment = "记录ID 值=create36time(9位)+UUID(32位)")
    private String payactid = "";

    @Column(length = 128, comment = "支付编号")
    private String payno = "";

    @Column(comment = "支付类型;10:银联;20:微信;30:支付宝;40:易宝;")
    private short paytype = 10;

    @Column(length = 1024, comment = "请求的URL")
    private String acturl = "";

    @Column(length = 2048, comment = "支付接口请求对象")
    private String requestjson = "";

    @Column(length = 5120, comment = "支付接口返回的原始结果")
    private String responsetext = "";

    @Column(updatable = false, comment = "创建时间，单位毫秒")
    private long createtime;

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

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

    public long getCreatetime() {
        return this.createtime;
    }

    public static class TableStrategy implements DistributeTableStrategy<PayAction> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, PayAction bean) {
            return getTable(table, bean.getCreatetime());
        }

        private String getTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return "redemo_pay_act." + table.substring(pos + 1) + "_" + String.format(format, createtime);
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getTable(table, Long.parseLong(id.substring(0, 9), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Range.LongRange createtime = (Range.LongRange) node.findValue("createtime");
            return getTable(table, createtime.getMin());
        }

    }
}
