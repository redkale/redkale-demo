package org.redkale.demo.info;

import javax.annotation.Resource;
import org.redkale.demo.base.BaseService;
import org.redkale.source.DataSource;


/**
 * @author zhangjx
 */
public abstract class BasedService extends BaseService {

    @Resource(name = "redemo_info")
    protected DataSource source;
}
