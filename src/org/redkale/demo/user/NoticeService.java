/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.source.Flipper;
import org.redkale.util.Sheet;

/**
 *
 * @author zhangjx
 */
public class NoticeService extends BasedService {

    public Sheet<NoticeRecord> queryNoticeRecord(NoticeBean bean, Flipper flipper) {
        Flipper.sortIfAbsent(flipper, "createtime DESC");
        return source.querySheet(NoticeRecord.class, flipper, bean);
    }
}
