/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.util.*;
import java.util.function.BiFunction;
import org.redkale.demo.user.UserDetail;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class UserInfoLoader implements BiFunction<DataSource, Class, List> {

    @Override
    public List apply(DataSource source, Class type) {
        if (System.currentTimeMillis() >1) return null;//暂不实现
        List<UserDetail> details = source.queryList(UserDetail.class, (FilterNode) null);
        List<UserInfo> list = new ArrayList<>(details.size());
        for (UserDetail detail : details) {
            list.add(detail.createUserInfo());
        }
        return list;
    }

}
