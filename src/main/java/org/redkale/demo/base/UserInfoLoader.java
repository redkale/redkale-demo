/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import org.redkale.demo.user.UserDetail;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
public class UserInfoLoader implements BiFunction<DataSource, EntityInfo, CompletableFuture<List>> {

    @Override
    public CompletableFuture<List> apply(DataSource source, EntityInfo info) {
        CompletableFuture<List<UserDetail>> future = source.queryListAsync(UserDetail.class, (FilterNode) null);
        return future.thenApply(details -> {
            List<UserInfo> list = new ArrayList<>(details.size());
            for (UserDetail detail : details) {
                list.add(detail.createUserInfo());
            }
            return list;
        });
    }

}
