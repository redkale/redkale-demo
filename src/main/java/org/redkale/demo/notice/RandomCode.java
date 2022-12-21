/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.notice;

import java.security.SecureRandom;
import org.redkale.demo.base.BaseEntity;
import org.redkale.persistence.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "有效验证码表")
public class RandomCode extends BaseEntity {

    private static final Reproduce<RandomCodeHis, RandomCode> reproduce = Reproduce.create(RandomCodeHis.class, RandomCode.class);

    private static final long serialVersionUID = 1L;

    private static final SecureRandom random = new SecureRandom();

    private static final char[] passwdsources = "023456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    private static final char[] char36sources = "023456789abcdefghjkmnpqrstuvwxyz".toCharArray();

    static {
        random.setSeed(Math.abs(System.nanoTime()));
    }

    //手机号码注册
    public static final short TYPE_SMSREG = 10;

    //短信重置密码
    public static final short TYPE_SMSPWD = 20;

    //修改手机号码
    public static final short TYPE_SMSMOB = 30;

    //用户验证码登录
    public static final short TYPE_SMSLGN = 40;

    //发送原手机号码
    public static final short TYPE_SMSODM = 50;

    @Id
    @Column(length = 128, comment = "手机-验证码数据对")
    private String randomcode;

    @Column(updatable = false, comment = "用户ID")
    private long userid; //用户ID

    @Column(comment = "验证码类型")
    private short type;

    @Column(comment = "创建时间")
    private long createtime;

    public RandomCode() {
    }

    public boolean isExpired() { //超过10分钟视为过期
        return System.currentTimeMillis() - createtime > 10 * 60 * 1000;
    }

    public RandomCodeHis createRandomCodeHis(int retcode) {
        RandomCodeHis his = reproduce.apply(new RandomCodeHis(), this);
        his.setRetcode(retcode);
        his.setUpdatetime(System.currentTimeMillis());
        his.setSeqid(Utility.format36time(his.getCreatetime()) + Utility.uuid());
        return his;
    }

    public static String randomPassword() {
        char[] chars = new char[8];
        int codesLen = passwdsources.length;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = passwdsources[random.nextInt(codesLen - 1)];
        }
        return new String(chars);
    }

    public static String random5Code() {
        char[] chars = new char[5];
        int codesLen = char36sources.length;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = char36sources[random.nextInt(codesLen - 1)];
        }
        return new String(chars);
    }

    public static String random6Code() {
        char[] chars = new char[6];
        int codesLen = char36sources.length;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = char36sources[random.nextInt(codesLen - 1)];
        }
        return new String(chars);
    }

    public static String random8Code() {
        char[] chars = new char[8];
        int codesLen = char36sources.length;
        for (int i = 0; i < chars.length; i++) {
            chars[i] = char36sources[random.nextInt(codesLen - 1)];
        }
        return new String(chars);
    }

    public static String randomLongCode() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new String(Utility.binToHex(bytes));
    }

    public static int randomSmsCode() {
        int rs = random.nextInt(9);
        if (rs == 0) rs = 1;
        for (int i = 0; i < 5; i++) {  //总长度为6
            rs = rs * 10 + random.nextInt(9);
        }
        return rs;
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

}
