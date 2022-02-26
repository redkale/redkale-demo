/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.pay;

import java.util.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import org.redkale.convert.json.*;
import org.redkale.demo.base.BaseService;
import org.redkale.service.RetResult;
import org.redkale.util.*;
import org.redkalex.pay.*;
import static org.redkalex.pay.PayRetCodes.*;

/**
 *
 * @author zhangjx
 */
@Comment("支付服务")
public class PayService extends BaseService {

    private static final JsonConvert convert = JsonFactory.create().skipAllIgnore(true).getConvert();

    @Resource
    private org.redkalex.pay.MultiPayService payService;

    public WeiXinPayService getWeiXinPayService() {
        return payService.getWeiXinPayService();
    }

    public UnionPayService getUnionPayService() {
        return payService.getUnionPayService();
    }

    public AliPayService getAliPayService() {
        return payService.getAliPayService();
    }

    @Comment("主动查询支付结果, 页面调用")
    public RetResult<PayRecord> checkPay(String payno) {
        return checkPay(findPayRecord(payno), true);
    }

    @Comment("固定返回支付成功结果， 用于调试")
    public RetResult<PayRecord> testCheckPay(String payno) {
        PayRecord pay = findPayRecord(payno);
        if (pay == null) return RetResult.success();
        pay.setPayedmoney(pay.getMoney());
        pay.setPaystatus(Pays.PAYSTATUS_PAYOK);
        pay.setFinishtime(System.currentTimeMillis());
        source.updateColumn(pay, "paystatus", "payedmoney", "finishtime");
        return new RetResult<>(pay);
    }

    @Comment("固定返回支付成功结果， 用于调试")
    public RetResult<Map<String, String>> testPrepay(final PayRecord pay) {
        pay.setCreatetime(System.currentTimeMillis());
        String create36time = Long.toString(pay.getCreatetime(), 36);
        if (create36time.length() < 9) create36time = "0" + create36time;
        pay.setPayno(pay.getOrderno() + create36time);
        PayPreRequest req = pay.createPayPreRequest();
        pay.setRequestjson(convert.convertTo(req));
        source.insert(pay);
        PayPreResponse rr = new PayPreResponse();
        if (!rr.isSuccess()) {
            pay.setPaystatus(Pays.PAYSTATUS_PAYNO);
            pay.setResponsetext(String.valueOf(rr));
            pay.setFinishtime(System.currentTimeMillis());
            source.updateColumn(pay, "paystatus", "responsetext", "finishtime");
        }
        if (rr.isSuccess()) rr.setRetinfo(pay.getPayno());
        rr.setResponsetext(""); //请求内容，防止信息暴露给外部接口
        return rr;
    }

    @Comment("主动查询支付结果, 定时任务调用")
    public RetResult<PayRecord> checkPay(PayRecord pay, final boolean forceFail) {
        if (pay == null) return RetResult.success();
        if (pay.getPaystatus() != Pays.PAYSTATUS_UNPAY && pay.getPaystatus() != Pays.PAYSTATUS_UNREFUND) return RetResult.success();//已经更新过了
        PayRequest request = new PayRequest(pay.getAppid(), pay.getPaytype(), pay.getPayway(), pay.getPayno());
        final PayQueryResponse resp = payService.query(request);
        PayAction payact = new PayAction();
        payact.setActurl(Pays.PAYACTION_QUERY);
        payact.setCreatetime(System.currentTimeMillis());
        payact.setPayno(pay.getPayno());
        payact.setPaytype(pay.getPaytype());
        payact.setRequestjson(convert.convertTo(request));
        payact.setResponsetext(convert.convertTo(resp));
        payact.setPayactid(Utility.format36time(payact.getCreatetime()) + Utility.uuid());
        source.insert(payact);
        if (resp.isSuccess()) { //查询结果成功，并不表示支付成功
            if (resp.getPayStatus() != Pays.PAYSTATUS_UNPAY //不能将未支付状态更新到pay中， 否则notify发现是未支付状态会跳过pay的更新
                && resp.getPayStatus() != Pays.PAYSTATUS_UNREFUND) {
                pay.setPaystatus(resp.getPayStatus());
            }
            if (pay.isPayok()) pay.setPayedmoney(pay.getMoney());
            pay.setThirdpayno(resp.getThirdPayno());
            pay.setFinishtime(payact.getCreatetime());
            pay.setResponsetext(payact.getResponsetext());
            source.updateColumn(pay, "paystatus", "payedmoney", "thirdpayno", "responsetext", "finishtime");
        } else if (forceFail || (pay.getCreatetime() + 3 * 60 * 1000 < System.currentTimeMillis())) { //超过3分钟视为支付失败
            pay.setPaystatus(Pays.PAYSTATUS_CLOSED);
            pay.setThirdpayno(resp.getThirdPayno());
            pay.setFinishtime(payact.getCreatetime());
            pay.setResponsetext(payact.getResponsetext());
            source.updateColumn(pay, "paystatus", "thirdpayno", "responsetext", "finishtime");
        }
        return new RetResult(resp.getRetcode(), resp.getRetinfo()).result(pay);
    }

    @Comment("手机支付回调")
    public RetResult<PayRecord> notify(final PayNotifyRequest request) {
        final PayNotifyResponse resp = payService.notify(request);
        PayRecord pay = findPayRecord(resp.getPayno());
        PayAction payact = new PayAction();
        payact.setActurl(Pays.PAYACTION_NOTIFY);
        payact.setCreatetime(System.currentTimeMillis());
        payact.setPayno(resp.getPayno());
        payact.setPaytype(resp.getPayType());
        payact.setRequestjson(convert.convertTo(request));
        payact.setResponsetext(convert.convertTo(resp));
        payact.setPayactid(Utility.format36time(payact.getCreatetime()) + Utility.uuid());
        source.insert(payact);
        if (pay.getPaystatus() != Pays.PAYSTATUS_UNPAY && pay.getPaystatus() != Pays.PAYSTATUS_UNREFUND) { //已经更新过了
            logger.log(Level.WARNING, "pay (" + pay + ") status error, req = " + request + ", resp = " + resp);
            return new RetResult(PayRetCodes.RETPAY_STATUS_ERROR, resp.getNotifyText()).result(pay);
        }
        if (resp.isSuccess()) { //支付成功
            pay.setPayedmoney(pay.getMoney());
            pay.setPaystatus(Pays.PAYSTATUS_PAYOK);
            pay.setThirdpayno(resp.getThirdpayno());
            pay.setFinishtime(System.currentTimeMillis());
            pay.setResponsetext(payact.getResponsetext());
            source.updateColumn(pay, "payedmoney", "paystatus", "thirdpayno", "responsetext", "finishtime");
        } else if (resp.getRetcode() != RETPAY_FALSIFY_ERROR && resp.getRetcode() != RETPAY_PAY_WAITING) {
            pay.setPaystatus(Pays.PAYSTATUS_PAYNO);
            pay.setThirdpayno(resp.getThirdPayno());
            pay.setFinishtime(System.currentTimeMillis());
            pay.setResponsetext(payact.getResponsetext());
            source.updateColumn(pay, "paystatus", "thirdpayno", "responsetext", "finishtime");
        }
        if (!resp.isSuccess()) return new RetResult(resp.getRetcode(), resp.getRetinfo()).result(pay);
        return new RetResult<>(pay).retinfo(resp.getNotifyText());   //支付的回调参数处理完必须输出success字样
    }

    @Comment("微信公众号、手机支付时调用")
    public RetResult<Map<String, String>> prepay(final PayRecord pay) {
        pay.setCreatetime(System.currentTimeMillis());
        String create36time = Long.toString(pay.getCreatetime(), 36);
        if (create36time.length() < 9) create36time = "0" + create36time;
        pay.setPayno(pay.getOrderno() + create36time);
        PayPreRequest req = pay.createPayPreRequest();
        pay.setRequestjson(convert.convertTo(req));
        source.insert(pay);
        PayPreResponse rr = payService.prepay(req);
        if (!rr.getAppid().isEmpty()) pay.setAppid(rr.getAppid());
        if (!rr.isSuccess()) {
            pay.setPaystatus(Pays.PAYSTATUS_PAYNO);
            pay.setResponsetext(String.valueOf(rr));
            pay.setFinishtime(System.currentTimeMillis());
            source.updateColumn(pay, "appid", "paystatus", "responsetext", "finishtime");
        }
        if (rr.isSuccess()) rr.setRetinfo(pay.getPayno());
        rr.setResponseText(""); //请求内容，防止信息暴露给外部接口
        return rr;
    }

    @Comment("根据payno查找单个PayRecord")
    public PayRecord findPayRecord(String payno) {
        return source.find(PayRecord.class, payno);
    }

}
