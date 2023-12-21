/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.pay;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import org.redkale.annotation.Resource;
import org.redkale.net.http.*;
import org.redkalex.pay.*;

/**
 *
 * @author zhangjx
 */
//@WebServlet({"/pay/*"})
public class PayServlet extends HttpServlet {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean fine = logger.isLoggable(Level.FINE);

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

    protected final boolean info = logger.isLoggable(Level.INFO);

    @Resource
    private PayService service;

    @HttpMapping(url = "/pay/check/", auth = false, comment = "根据支付单号检查支付结果") ///pay/check/{payno}
    public void checkPay(HttpRequest req, HttpResponse resp) throws IOException {
        if (info) {
            logger.info("" + req);
        }
        String payno = req.getPathLastParam();
        resp.finishJson(service.checkPay(payno));
    }

    public static PayNotifyRequest unionNotifyRequest(Logger logger, HttpRequest req) throws IOException {
        logger.info("" + req);
        final TreeMap<String, String> map = new TreeMap<>();
        map.put("version", req.getParameter("version", "")); //不可空 版本号
        map.put("encoding", req.getParameter("encoding", "")); //不可空 编码方式
        map.put("certId", req.getParameter("certId", "")); //不可空 证书 ID
        map.put("signature", req.getParameter("signature", "")); //不可空 签名 
        map.put("signMethod", req.getParameter("signMethod", "")); //不可空 签名方法
        map.put("txnType", req.getParameter("txnType", "")); //交易类型
        map.put("txnSubType", req.getParameter("txnSubType", "")); //交易子类
        map.put("bizType", req.getParameter("bizType", "")); //产品类型
        map.put("accessType", req.getParameter("accessType", "")); //接入类型
        map.put("merId", req.getParameter("merId", "")); //商户代码
        map.put("orderId", req.getParameter("orderId", "")); //商户订单号
        map.put("txnTime", req.getParameter("txnTime", "")); //订单发送时间
        map.put("txnAmt", req.getParameter("txnAmt", "")); //交易金额
        map.put("currencyCode", req.getParameter("currencyCode", "")); //交易币种
        map.put("reqReserved", req.getParameter("reqReserved", "")); //请求方保留域
        map.put("reserved", req.getParameter("reserved", "")); //保留域
        map.put("queryId", req.getParameter("queryId", "")); //交易查询流水号 消费交易的流水号，供后续查询用
        map.put("respCode", req.getParameter("respCode", "")); //响应码
        map.put("respMsg", req.getParameter("respMsg", "")); //响应信息
        map.put("settleAmt", req.getParameter("settleAmt", "")); //清算金额
        map.put("settleCurrencyCode", req.getParameter("settleCurrencyCode", "")); //清算币种
        map.put("settleDate", req.getParameter("settleDate", "")); //清算日期
        map.put("traceNo", req.getParameter("traceNo", "")); //系统跟踪号
        map.put("traceTime", req.getParameter("traceTime", "")); //交易传输时间
        map.put("exchangeDate", req.getParameter("exchangeDate", "")); //兑换日期
        map.put("exchangeRate", req.getParameter("exchangeRate", "")); //汇率
        map.put("accNo", req.getParameter("accNo", "")); //账号
        map.put("payCardType", req.getParameter("payCardType", "")); //支付卡类型
        map.put("payType", req.getParameter("payType", "")); //支付方式
        map.put("payCardNo", req.getParameter("payCardNo", "")); //支付卡标识 移动支付交易时，根据商户配置返回
        map.put("payCardIssueName", req.getParameter("payCardIssueName", "")); //支付卡名称 移动支付交易时，根据商户配置返回
        map.put("bindId", req.getParameter("bindId", "")); //绑定标识号 绑定支付时，根据商户配置返回

        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, String> en : map.entrySet()) { //去掉空值的参数
            if (en.getValue().isEmpty()) {
                emptyKeys.add(en.getKey());
            }
        }
        emptyKeys.forEach(x -> map.remove(x));

        return new PayNotifyRequest(Pays.PAYTYPE_UNION, map);
    }

    @HttpMapping(url = "/pay/union/notify", auth = false, comment = "银联支付异步通知")
    public void unionnotify(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish(service.notify(unionNotifyRequest(logger, req)).getRetinfo());
    }

    public static PayNotifyRequest weixinNotifyRequest(Logger logger, HttpRequest req) throws IOException {
        String body = req.getBodyUTF8();
        logger.info("" + req + "; body = " + body);
        return new PayNotifyRequest(Pays.PAYTYPE_WEIXIN, body);
    }

    @HttpMapping(url = "/pay/weixin/notify", auth = false, comment = "微信支付异步通知")
    public void weixinnotify(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish(service.notify(weixinNotifyRequest(logger, req)).getRetinfo());
    }

    public static PayNotifyRequest alipayNotifyRequest(PayService service, Logger logger, HttpRequest req) throws IOException {
        logger.info("" + req);
        final TreeMap<String, String> map = new TreeMap<>();
        map.put("notify_time", req.getParameter("notify_time", "")); //不可空 通知的发送时间。格式为yyyy-MM-dd HH:mm:ss。
        map.put("notify_type", req.getParameter("notify_type", "")); //不可空 通知的类型。
        map.put("notify_id", req.getParameter("notify_id", "")); //不可空 通知校验ID。
        map.put("sign_type", req.getParameter("sign_type", "")); //不可空 固定取值为RSA。
        map.put("sign", req.getParameter("sign", "")); //不可空 签名
        map.put("out_trade_no", req.getParameter("out_trade_no", "")); //可空  对应商户网站的订单系统中的唯一订单号，非支付宝交易号。需保证在商户网站中的唯一性。是请求时对应的参数，原样返回。
        map.put("subject", req.getParameter("subject", "")); //可空 商品的标题/交易标题/订单标题/订单关键字等。它在支付宝的交易明细中排在第一列，对于财务对账尤为重要。是请求时对应的参数，原样通知回来
        map.put("payment_type", req.getParameter("payment_type", "")); //可空 支付类型。默认值为：1（商品购买）。
        map.put("trade_no", req.getParameter("trade_no", "")); //不可空 该交易在支付宝系统中的交易流水号。最短16位，最长64位。

        //交易状态:
        //WAIT_BUYER_PAY	交易创建，等待买家付款。
        //TRADE_CLOSED	在指定时间段内未支付时关闭的交易； 在交易完成全额退款成功时关闭的交易。
        //TRADE_SUCCESS	交易成功，且可对该交易做操作，如：多级分润、退款等。
        //TRADE_FINISHED	交易成功且结束，即不可再做任何操作。
        map.put("trade_status", req.getParameter("trade_status", "")); //不可空 交易状态，取值范围请参见“交易状态”。
        map.put("seller_id", req.getParameter("seller_id", "")); //不可空 卖家支付宝账号对应的支付宝唯一用户号。以2088开头的纯16位数字。
        map.put("seller_email", req.getParameter("seller_email", "")); //不可空 卖家支付宝账号，可以是email和手机号码。
        map.put("buyer_id", req.getParameter("buyer_id", "")); //不可空 买家支付宝账号对应的支付宝唯一用户号。以2088开头的纯16位数字。
        map.put("buyer_email", req.getParameter("buyer_email", "")); //不可空 买家支付宝账号，可以是Email或手机号码。
        map.put("total_fee", req.getParameter("total_fee", "0")); //不可空  该笔订单的总金额。请求时对应的参数，原样通知回来。
        map.put("quantity", req.getParameter("quantity", "")); //可空 购买数量，固定取值为1（请求时使用的是total_fee）。
        map.put("price", req.getParameter("price", "")); //可空 price等于total_fee（请求时使用的是total_fee）。
        map.put("body", req.getParameter("body", "")); //可空 该笔订单的备注、描述、明细等。对应请求时的body参数，原样通知回来。
        map.put("gmt_create", req.getParameter("gmt_create", "")); //可空 该笔交易创建的时间。格式为yyyy-MM-dd HH:mm:ss。
        map.put("gmt_payment", req.getParameter("gmt_payment", "")); //可空 该笔交易的买家付款时间。格式为yyyy-MM-dd HH:mm:ss。
        map.put("is_total_fee_adjust", req.getParameter("is_total_fee_adjust", "")); //可空 该交易是否调整过价格。
        map.put("use_coupon", req.getParameter("use_coupon", "")); //可空 是否在交易过程中使用了红包。
        map.put("discount", req.getParameter("discount", "")); //可空 支付宝系统会把discount的值加到交易金额上，如果有折扣，本参数为负数，单位为元。
        map.put("refund_status", req.getParameter("refund_status", "")); //可空 取值范围请参见“退款状态”。
        map.put("gmt_refund", req.getParameter("gmt_refund", "")); //可空 卖家退款的时间，退款通知时会发送。格式为yyyy-MM-dd HH:mm:ss。

        List<String> emptyKeys = new ArrayList<>();
        for (Map.Entry<String, String> en : map.entrySet()) { //去掉空值的参数
            if (en.getValue().isEmpty()) {
                emptyKeys.add(en.getKey());
            }
        }
        emptyKeys.forEach(x -> map.remove(x));

        PayNotifyRequest bean = new PayNotifyRequest(Pays.PAYTYPE_ALIPAY, map);
        if (service != null) {
            try {
                PayRecord pay = service.findPayRecord(map.get("out_trade_no"));
                if (pay != null) {
                    bean.setAppid(pay.getAppid());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "find appid form payrecord(" + map + ") error", e);
            }
        }
        return bean;
    }

    @HttpMapping(url = "/pay/alipay/notify", auth = false, comment = "支付宝支付异步通知")
    public void alipaynotify(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish(service.notify(alipayNotifyRequest(service, logger, req)).getRetinfo());
    }

}
