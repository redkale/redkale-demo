/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.io.IOException;
import java.net.URLEncoder;
import javax.annotation.Resource;
import org.redkale.source.Flipper;
import org.redkale.util.Sheet;

/**
 * 发送短信服务
 *
 * @author zhangjx
 */
public class SmsService extends BasedService {

    @Resource(name = "property.sms.sendurl")
    private String smssendurl = "http://xxxxx";

    @Resource(name = "property.sms.account")
    private String smsaccount = "xxxxxxx";

    @Resource(name = "property.sms.password")
    private String smspassword = "yyyy";

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
            source.insert(message);
        }
        return ok;
    }

    public Sheet<SmsRecord> querySmsRecord(SmsBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(SmsRecord.class, flipper, bean);
    }
}
