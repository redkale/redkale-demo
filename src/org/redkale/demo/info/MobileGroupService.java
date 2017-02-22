/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.info;

/**
 *
 * @author zhangjx
 */
public class MobileGroupService extends BasedService {

    public MobileGroup findMobileGroup(final String mobile) {
        return source.find(MobileGroup.class, (mobile.length() > 7) ? mobile.substring(0, 7) : mobile);
    }
}
