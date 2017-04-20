/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.util.logging.*;
import org.redkale.service.*;

/**
 *
 * @author zhangjx
 */
public abstract class BaseService extends AbstractService {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

}
