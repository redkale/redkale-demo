/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import org.redkale.annotation.Comment;
import org.redkale.demo.base.BaseService;
import org.redkale.source.Flipper;
import org.redkale.util.Sheet;

/**
 *
 * @author zhangjx
 */

@Comment("消息推送服务")
public class NoticeService extends BaseService {

    public Sheet<NoticeRecord> queryNoticeRecord(NoticeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createTime DESC");
        return source.querySheet(NoticeRecord.class, flipper, bean);
    }
}
