/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.security.SecureRandom;
import javax.persistence.*;
import org.redkale.demo.base.BaseEntity;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
@Entity
public class RandomCode extends BaseEntity {

    private static final Reproduce<RandomCodeHis, RandomCode> reproduce = Reproduce.create(RandomCodeHis.class, RandomCode.class);

    private static final long serialVersionUID = 1L;

    private static final SecureRandom random = new SecureRandom();

    private static final char[] passwdsources = "023456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

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

    //邮件重置密码
    public static final short TYPE_MAILPWD = 60;

    //更改邮箱绑定
    public static final short TYPE_MAILBIND = 70;

    @Id
    private String randomcode;

    private int userid;

    private short type;

    private long createtime;

    public RandomCode() {
    }

    public boolean isExpired() { //邮箱类型24小时过期， 其他10分钟过期
        long s = (type == TYPE_MAILPWD || type == TYPE_MAILBIND) ? 24 * 60 * 60 * 1000 : 10 * 60 * 1000;
        return System.currentTimeMillis() - createtime > s;
    }

    public RandomCodeHis createRandomCodeHis(int retcode) {
        RandomCodeHis his = new RandomCodeHis();
        reproduce.copy(his, this);
        his.setRetcode(retcode);
        his.setSeqid(Math.abs(System.nanoTime()));
        his.setUpdatetime(System.currentTimeMillis());
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
