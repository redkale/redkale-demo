/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import java.io.IOException;
import java.net.URLEncoder;
import javax.annotation.Resource;
import org.redkale.demo.base.BaseService;
import org.redkale.source.Flipper;
import org.redkale.util.*;

/**
 * 发送短信服务
 *
 *
 * @author zhangjx
 */
@Comment("短信服务")
public class SmsService extends BaseService {

    @Resource(name = "property.sms.sendurl")
    protected String smssendurl = "http://xxxxx";

    @Resource(name = "property.sms.account")
    protected String smsaccount = "xxxxxxx";

    @Resource(name = "property.sms.password")
    protected String smspassword = "yyyy";

    public boolean sendRandomSmsCode(short smstype, String mobile, int randomSmsCode) {
        return sendSmsRecord(smstype, mobile, "【应用名称】验证码为: " + randomSmsCode + "");
    }

    public boolean sendSmsRecord(short smstype, String mobile, String content0) {
        final SmsRecord message = new SmsRecord(smstype, mobile, content0);
        String content = message.getContent();
        String resultdesc = "encode_content_error";
        boolean ok = false;
        try {
            content = URLEncoder.encode(content, "UTF-8");
//            //发送短信消息
//            String body = "cdkey=" + smsaccount + "&password=" + smspassword + "&phone=" + message.getMobile()
//                + "&message=" + content + "&seqid=" + Math.abs(System.nanoTime());
//            resultdesc = "send_timeout";
//            resultdesc = Utility.postHttpContent(smssendurl + "?" + body);
            ok = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        message.setResultdesc(resultdesc);
        if (source != null) {
            message.setStatus(ok ? SmsRecord.SMSSTATUS_SENDOK : SmsRecord.SMSSTATUS_SENDNO);
            message.setSmsid(Utility.format36time(message.getCreatetime()) + Utility.uuid());
            source.insert(message);
        }
        return ok;
    }

    @Comment("查询短信记录列表")
    public Sheet<SmsRecord> querySmsRecord(@Comment("过滤条件") SmsBean bean, @Comment("翻页对象") Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(SmsRecord.class, flipper, bean);
    }
}
