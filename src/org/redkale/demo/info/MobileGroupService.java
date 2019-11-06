/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.info;

import org.redkale.demo.base.BaseService;
import org.redkale.util.Comment;

/**
 *
 * @author zhangjx
 */
@Comment("手机所属地服务模块")
public class MobileGroupService extends BaseService {

    @Comment("根据手机号查询手机所属地信息")
    public MobileGroup findMobileGroup(@Comment("11位手机号码") final String mobile) {
        return source.find(MobileGroup.class, (mobile.length() > 7) ? mobile.substring(0, 7) : mobile);
    }
}
