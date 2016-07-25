package org.redkale.demo.user;

import javax.annotation.Resource;
import org.redkale.demo.base.BaseService;
import org.redkale.source.DataSource;

/**
 * @author zhangjx
 */
public abstract class BasedService extends BaseService {

    @Resource(name = "reduser")
    protected DataSource source;
}
