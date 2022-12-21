/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import java.io.Serializable;
import org.redkale.demo.base.BaseEntity;
import org.redkale.persistence.*;
import org.redkale.source.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "验证码历史表")
@DistributeTable(strategy = RandomCodeHis.TableStrategy.class)
public class RandomCodeHis extends BaseEntity {

    private static final long serialVersionUID = 1L;

    //过期
    public static final short RETCODE_EXP = 2;

    //已处理
    public static final short RETCODE_OK = 4;

    @Id
    @Column(length = 64, comment = "记录ID 值=create36time(9位)+UUID(32位)")
    private String seqid = "";

    @Column(length = 128, comment = "手机-验证码数据对")
    private String randomcode;

    @Column(updatable = false, comment = "[所属用户ID]")
    private long userid; //用户ID

    @Column(comment = "验证码类型")
    private short type;

    @Column(comment = "创建时间")
    private long createtime;

    @Column(comment = "结果码，0为成功")
    private int retcode;

    @Column(comment = "更新时间")
    private long updatetime;

    public String getSeqid() {
        return seqid;
    }

    public void setSeqid(String seqid) {
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

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
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

    public static class TableStrategy implements DistributeTableStrategy<RandomCodeHis> {

        private static final String format = "%1$tY%1$tm";

        @Override
        public String getTable(String table, RandomCodeHis bean) {
            return table + "_" + String.format(format, bean.getCreatetime());
        }

        @Override
        public String getTable(String table, Serializable primary) {
            String id = (String) primary;
            return getTable(table, Long.parseLong(id.substring(0, 9), 36));
        }

        @Override
        public String getTable(String table, FilterNode node) {
            Range.LongRange createtime = (Range.LongRange) node.findValue("createtime");
            return getTable(table, createtime.getMin());
        }

        private String getTable(String table, long createtime) {
            int pos = table.indexOf('.');
            return "redemo_notice." + table.substring(pos + 1) + "_" + String.format(format, createtime);
        }
    }
}
