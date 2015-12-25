/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.io.*;
import org.redkale.convert.json.*;

/**
 *
 * @author zhangjx
 */
public abstract class BaseEntity implements Serializable {

    //正常
    public static final short STATUS_NORMAL = 10;

    //待审批
    public static final short STATUS_PENDING = 20;

    //冻结
    public static final short STATUS_FREEZE = 40;

    //隐藏
    public static final short STATUS_HIDDEN = 50;

    //关闭
    public static final short STATUS_CLOSED = 60;

    //过期
    public static final short STATUS_EXPIRE = 70;

    //删除
    public static final short STATUS_DELTED = 80;

    @Override
    public String toString() {
        return JsonFactory.root().getConvert().convertTo(this);
    }

}
