/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.pay;

import javax.annotation.Resource;
import org.redkale.demo.base.BaseService;
import org.redkale.source.DataSource;

/**
 *
 * @author zhangjx
 */
public abstract class BasedService extends BaseService {

    @Resource(name = "redemo_notice")
    protected DataSource source;
}
