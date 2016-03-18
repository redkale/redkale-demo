/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;

/**
 *
 * @author zhangjx
 */
@Entity
public class RandomCodeHis extends BaseEntity {

    private static final long serialVersionUID = 1L;

    //过期
    public static final short RETCODE_EXP = 2;

    //已处理
    public static final short RETCODE_OK = 4;

    @Id
    @GeneratedValue
    private long seqid;

    private String randomcode;

    private int userid;

    private short type;

    private long createtime;

    private int retcode;

    private long updatetime;

    public long getSeqid() {
        return seqid;
    }

    public void setSeqid(long seqid) {
        this.seqid = seqid;
    }

    public int getRetcode() {
        return retcode;
    }

    public void setRetcode(int retcode) {
        this.retcode = retcode;
    }

    public long getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(long updatetime) {
        this.updatetime = updatetime;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getRandomcode() {
        return randomcode;
    }

    public void setRandomcode(String randomcode) {
        this.randomcode = randomcode;
    }

    public long getCreatetime() {
        return createtime;
    }

    public void setCreatetime(long createtime) {
        this.createtime = createtime;
    }

}
