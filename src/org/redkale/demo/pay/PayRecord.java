package org.redkale.demo.pay;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;
import org.redkale.demo.base.BaseEntity;
import org.redkale.source.*;
import org.redkalex.pay.*;

/**
 *
 * @author zhangjx
 */
@Table(catalog = "redemo_pay", comment = "支付表")
@DistributeTable(strategy = PayRecord.TableStrategy.class)
public class PayRecord extends BaseEntity {

    @Id
    @Column(length = 64, comment = "支付编号; 值=orderno+createtime36进制(9位)")
    private String payno = "";

    @Column(length = 128, comment = "第三方支付订单号")
    private String thirdpayno = "";

    @Column(length = 128, comment = "支付APP应用ID")
    private String appid = "";

    @Column(comment = "支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;")
    private short paytype = 10;

    @Column(comment = "支付渠道:  10: 信用/虚拟支付; 20:人工支付; 30:APP支付; 40:网页支付; 50:机器支付;")
    private short payway = 10;

    @Column(length = 64, comment = "付款人用户信息")
    private String userno = "";

    @Column(length = 128, comment = "订单标题")
    private String paytitle = "";

    @Column(length = 255, comment = "订单内容描述")
    private String paybody = "";

    @Column(length = 255, comment = "支付回调连接")
    private String notifyurl = "";

    @Column(comment = "订单类型")
    private short ordertype;

    @Column(length = 64, comment = "订单编号")
    private String orderno = "";

    @Column(comment = "支付状态; 10:待支付; 30:已支付; 50:待退款; 70:已退款; 90:已关闭;")
    private short paystatus = 10;

    @Column(comment = "实际支付金额 单位人民币分；")
    private long payedmoney;

    @Column(comment = "订单金额，单位人民币分；")
    private long money;

    @Column(length = 1024, comment = "支付接口请求对象")
    private String requestjson = "";

    @Column(length = 10240, comment = "支付接口返回的原始结果")
    private String responsetext = "";

    @Column(updatable = false, comment = "支付开始时间，单位毫秒")
    private long createtime;

    @Column(comment = "支付结束时间，单位毫秒")
    private long finishtime;

    @Column(updatable = false, length = 64, comment = "客户端请求的HOST")
    private String clienthost = "";
    
    @Column(length = 128, comment = "客户端生成时的IP")
    private String clientaddr = "";

    @Transient
    private Map<String, String> map; //扩展信息

    public PayRecord() {
    }

    public PayPreRequest createPayPreRequest() {
        PayPreRequest req = new PayPreRequest();
        req.setPayno(this.getPayno());
        req.setMap(this.getMap());
        req.setPaytype(this.getPaytype());
        req.setPayway(this.getPayway());
        req.setPaymoney(this.getMoney());
        req.setAppid(this.getAppid());
        req.setClienthost(this.getClienthost());
        req.setClientAddr(this.getClientaddr());
        req.setPaytitle(this.getPaytitle());
        req.setPaybody(this.getPaybody());
        req.setNotifyurl(this.getNotifyurl());
        return req;
    }

    public boolean isPayok() {
        return this.paystatus == Pays.PAYSTATUS_PAYOK;
    }

    public boolean isRefundok() {
        return this.paystatus == Pays.PAYSTATUS_REFUNDOK;
    }

    public void setPayno(String payno) {
        this.payno = payno;
    }

    public String getPayno() {
        return this.payno;
    }

    public void setThirdpayno(String thirdpayno) {
        this.thirdpayno = thirdpayno;
    }

    public String getThirdpayno() {
        return this.thirdpayno;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppid() {
        return this.appid == null ? "" : this.appid;
    }

    public void setPaytype(short paytype) {
        this.paytype = paytype;
    }

    public short getPaytype() {
        return this.paytype;
    }

    public String getPaytypename() {
        if (this.paytype == Pays.PAYTYPE_UNION) return "union";
        if (this.paytype == Pays.PAYTYPE_WEIXIN) return "weixin";
        if (this.paytype == Pays.PAYTYPE_ALIPAY) return "alipay";
        return "xxx"; //不存在的类型
    }

    public void setPayway(short payway) {
        this.payway = payway;
    }

    public short getPayway() {
        return this.payway;
    }

    public void setUserno(String userno) {
        this.userno = userno;
    }

    public String getUserno() {
        return this.userno;
    }

    public String getPaytitle() {
        return paytitle;
    }

    public void setPaytitle(String paytitle) {
        this.paytitle = paytitle;
    }

    public String getPaybody() {
        return paybody;
    }

    public void setPaybody(String paybody) {
        this.paybody = paybody;
    }

    public String getNotifyurl() {
        return notifyurl;
    }

    public void setNotifyurl(String notifyurl) {
        this.notifyurl = notifyurl;
    }

    public short getOrdertype() {
        return ordertype;
    }

    public void setOrdertype(short ordertype) {
        this.ordertype = ordertype;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getOrderno() {
        return this.orderno;
    }

    public void setPaystatus(short paystatus) {
        this.paystatus = paystatus;
    }

    public short getPaystatus() {
        return this.paystatus;
    }

    public void setPayedmoney(long payedmoney) {
        this.payedmoney = payedmoney;
    }

    public long getPayedmoney() {
        return this.payedmoney;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getMoney() {
        return this.money;
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

    public void setFinishtime(long finishtime) {
        this.finishtime = finishtime;
    }

    public long getFinishtime() {
        return this.finishtime;
    }

    public String getClienthost() {
        return clienthost;
    }

    public void setClienthost(String clienthost) {
        this.clienthost = clienthost;
    }

    public void setClientaddr(String clientaddr) {
        this.clientaddr = clientaddr;
    }

    public String getClientaddr() {
        return this.clientaddr;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public static class TableStrategy implements DistributeTableStrategy<PayRecord> {

        private static final String format = "%1$tY%1$tm%1$td";

        @Override
        public String getTable(String table, Serializable primary) {
            final String id = (String) primary;
            return getTable(table, Long.parseLong(id.substring(id.length() - 9), 36));
        }

        private String getTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return "redemo_pay." + table.substring(pos + 1) + "_" + String.format(format, createtime);
        }

        @Override
        public String getTable(String table, PayRecord bean) {
            return getTable(table, bean.getPayno());
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Range.LongRange createtime = (Range.LongRange) node.findValue("createtime");
            return getTable(table, createtime.getMin());
        }

    }
}
