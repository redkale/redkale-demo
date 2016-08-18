/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import org.redkale.convert.json.JsonConvert;
import static org.redkale.demo.base.RetCodes.*;
import org.redkale.demo.base.*;
import static org.redkale.demo.base.UserInfo.*;
import org.redkale.demo.file.FileService;
import static org.redkale.demo.user.UserDetail.*;
import org.redkalex.email.EmailService;
import org.redkalex.weixin.WeiXinMPService;
import org.redkale.service.*;
import org.redkale.source.*;
import static org.redkale.source.FilterExpress.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class UserService extends BasedService {

    private static final MessageDigest sha1;

    private static final MessageDigest md5;

    public static final String AES_KEY = "REDKALE_20160202";

    private static final Cipher aesEncrypter; //加密

    private static final Cipher aesDecrypter; //解密

    static {
        MessageDigest d = null;
        try {
            d = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        sha1 = d;
        try {
            d = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        md5 = d;

        Cipher cipher = null;
        final SecretKeySpec aesKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        } catch (Exception e) {
            throw new Error(e);
        }
        aesEncrypter = cipher;  //加密
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
        } catch (Exception e) {
            throw new Error(e);
        }
        aesDecrypter = cipher; //解密
    }

    protected final AtomicInteger maxid = new AtomicInteger(200000000);

    private final int sessionExpireSeconds = 30 * 60;

    @Resource(name = "usersessions")
    protected CacheSource<String, Integer> sessions;

    @Resource
    protected FileService fileService;

    @Resource
    private WeiXinMPService wxMPService;

    @Resource
    private EmailService emailService;

    @Resource
    private SmsService smsService;

    @Resource
    private JsonConvert convert;

    //private final String userbundle = this.getClass().getPackage().getName() + ".userbundle";
    @Override
    public void init(AnyValue conf) {
        updateMax0(); //init里不能调用@MultiRun方法
    }

    private boolean updateMax0() {
        boolean rs = false;
        Number max = source.getNumberResult(UserDetail.class, FilterFunc.MAX, "userid");
        if (max != null && max.intValue() > 200000000) maxid.set(max.intValue());
        rs |= max != null;
        return rs;
    }

    @MultiRun
    public boolean updateMax() {
        return updateMax0();
    }

    @Override
    public void destroy(AnyValue conf) {
    }

    //根据用户ID查找用户
    public UserInfo findUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM;
        return source.find(UserInfo.class, userid);
    }

    //根据账号查找用户
    public UserInfo findUserInfoByAccount(String account) {
        if (account == null || account.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("account", IGNORECASEEQUAL, account));
    }

    //根据手机号码查找用户
    public UserInfo findUserInfoByMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("mobile", EQUAL, mobile));
    }

    //根据邮箱地址查找用户
    public UserInfo findUserInfoByEmail(String email) {
        if (email == null || email.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("email", IGNORECASEEQUAL, email));
    }

    //根据微信绑定ID查找用户
    public UserInfo findUserInfoByWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("wxunionid", EQUAL, wxunionid));
    }

    //根据QQ绑定ID查找用户
    public UserInfo findUserInfoByQqopenid(String qqopenid) {
        if (qqopenid == null || qqopenid.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("qqopenid", EQUAL, qqopenid));
    }

    //根据APP设备ID查找用户
    public UserInfo findUserInfoByApptoken(String apptoken) {
        if (apptoken == null || apptoken.isEmpty()) return null;
        return source.find(UserInfo.class, FilterNode.create("apptoken", EQUAL, apptoken));
    }

    //查询用户列表， 通常用于后台管理系统查询
    public Sheet<UserDetail> queryUserDetail(FilterNode node, Flipper flipper) {
        return source.querySheet(UserDetail.class, flipper, node);
    }

    //根据登录态获取当前用户信息
    public UserInfo current(String sessionid) {
        Integer userid = sessions.getAndRefresh(sessionid, sessionExpireSeconds);
        return userid == null ? null : findUserInfo(userid);
    }

    /**
     * 发送短信验证码
     *
     * @param type
     * @param mobile
     *
     * @return
     */
    public RetResult smscode(final short type, String mobile) {
        if (mobile == null) return new RetResult(RET_USER_MOBILE_ILLEGAL, type + " mobile is null"); //手机号码无效
        if (mobile.indexOf('+') == 0) mobile = mobile.substring(1);
        UserInfo info = findUserInfoByMobile(mobile);
        if (type == RandomCode.TYPE_SMSREG || type == RandomCode.TYPE_SMSMOB) { //手机注册或手机修改的号码不能已存在
            if (info != null) return new RetResult(RET_USER_MOBILE_EXISTS, "smsreg or smsmob mobile " + mobile + " exists");
        } else if (type == RandomCode.TYPE_SMSPWD) { //修改密码
            if (info == null) return new RetResult(RET_USER_NOTEXISTS, "smspwd mobile " + mobile + " not exists");
        } else if (type == RandomCode.TYPE_SMSLGN) { //手机登录
            if (info == null) return new RetResult(RET_USER_NOTEXISTS, "smslgn mobile " + mobile + " not exists");
        } else if (type == RandomCode.TYPE_SMSODM) { //原手机
            if (info == null) return new RetResult(RET_USER_NOTEXISTS, "smsodm mobile " + mobile + " not exists");
        } else {
            return new RetResult(RET_PARAMS_ILLEGAL, type + " is illegal");
        }
        List<RandomCode> codes = source.queryList(RandomCode.class, FilterNode.create("randomcode", FilterExpress.LIKE, mobile + "-%"));
        if (!codes.isEmpty()) {
            RandomCode last = codes.get(codes.size() - 1);
            if (last.getCreatetime() + 60 * 1000 > System.currentTimeMillis()) return RetCodes.retResult(RET_USER_MOBILE_SMSFREQUENT);
        }
        final int smscode = RandomCode.randomSmsCode();
        try {
            if (!smsService.sendRandomSmsCode(type, mobile, smscode)) return retResult(RET_USER_MOBILE_SMSFREQUENT);
        } catch (Exception e) {
            logger.log(Level.WARNING, "mobile(" + mobile + ", type=" + type + ") send smscode " + smscode + " error", e);
            return retResult(RET_USER_MOBILE_SMSFREQUENT);
        }
        RandomCode code = new RandomCode();
        code.setCreatetime(System.currentTimeMillis());
        if (info != null) code.setUserid(info.getUserid());
        code.setRandomcode(mobile + "-" + smscode);
        code.setType(type);
        source.insert(code);
        return RetResult.success();
    }

    //QQ登录
    public RetResult<UserInfo> qqlogin(LoginQQBean bean) {
        try {
            String qqappid = "xxxx";
            String url = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + qqappid + "&access_token=" + bean.getAccesstoken() + "&openid=" + bean.getOpenid() + "&format=json";
            String json = Utility.getHttpContent(url);
            if (finest) logger.finest(url + "--->" + json);
            Map<String, String> jsonmap = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, json);
            if (!"0".equals(jsonmap.get("ret"))) return RetCodes.retResult(RET_USER_QQID_INFO_FAIL);
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByQqopenid(bean.getOpenid());
            if (user == null) {
                UserDetail detail = new UserDetail();
                detail.setUsername(jsonmap.getOrDefault("nickname", "qq-user"));
                detail.setQqopenid(bean.getOpenid());
                detail.setRegagent(bean.getLoginagent());
                detail.setRegaddr(bean.getLoginaddr());
                String genstr = jsonmap.getOrDefault("gender", "");
                detail.setGender("男".equals(genstr) ? UserInfo.GENDER_MALE : ("女".equals(genstr) ? UserInfo.GENDER_FEMALE : (short) 0));
                if (finer) logger.fine(bean + " --qqlogin-->" + convert.convertTo(jsonmap));
                rr = register(detail);
                if (rr.isSuccess()) {
                    rr.setRetinfo(jsonmap.get(bean.getOpenid()));
                    String headimgurl = jsonmap.get("figureurl_qq_2");
                    if (headimgurl != null) {
                        super.submit(() -> {
                            try {
                                byte[] bytes = Utility.getHttpBytesContent(headimgurl);
                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                                fileService.storeFace(rr.getResult().getUserid(), image);
                            } catch (Exception e) {
                                logger.log(Level.INFO, "qqlogin get headimgurl fail (" + rr.getResult() + ", " + jsonmap + ")", e);
                            }
                        });
                    }
                }
            } else {
                rr = new RetResult<>(user);
                rr.setRetinfo(jsonmap.get(bean.getOpenid()));
            }
            if (rr.isSuccess()) {
                this.sessions.set(sessionExpireSeconds, bean.getSessionid(), rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "qqlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    //微信登陆
    public RetResult<UserInfo> wxlogin(LoginWXBean bean) {
        try {
            Map<String, String> wxmap = bean.emptyAccesstoken()
                ? wxMPService.getMPUserTokenByCode(bean.getCode())
                : wxMPService.getMPUserTokenByOpenid(bean.getAccesstoken(), bean.getOpenid());
            final String unionid = wxmap.get("unionid");
            if (unionid == null) return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByWxunionid(unionid);
            if (user == null) {
                if (!bean.isAutoreg()) return new RetResult(0, convert.convertTo(wxmap));
                UserDetail detail = new UserDetail();
                detail.setUsername(wxmap.getOrDefault("nickname", "wx-user"));
                detail.setWxunionid(unionid);
                detail.setApptoken(bean.getApptoken());
                detail.setRegagent(bean.getLoginagent());
                detail.setRegaddr(bean.getLoginaddr());
                detail.setGender((short) (Short.parseShort(wxmap.getOrDefault("sex", "0")) * 2));
                logger.fine(bean + " --wxlogin-->" + convert.convertTo(wxmap));
                rr = register(detail);
                if (rr.isSuccess()) {
                    rr.setRetinfo(wxmap.get("openid"));
                    String headimgurl = wxmap.get("headimgurl");
                    if (headimgurl != null) {
                        super.submit(() -> {
                            try {
                                byte[] bytes = Utility.getHttpBytesContent(headimgurl);
                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                                fileService.storeFace(rr.getResult().getUserid(), image);
                            } catch (Exception e) {
                                logger.log(Level.INFO, "wxlogin get headimgurl fail (" + rr.getResult() + ", " + wxmap + ")", e);
                            }
                        });
                    }
                }
            } else {
                rr = new RetResult<>(user);
                rr.setRetinfo(wxmap.get("openid"));
                if (!user.getApptoken().equals(bean.getApptoken())) {
                    user.setApptoken(bean.getApptoken());
                    source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", bean.getApptoken());
                    source.updateColumn(UserInfo.class, user.getUserid(), "apptoken", bean.getApptoken());
                }
            }
            if (rr.isSuccess()) {
                this.sessions.set(sessionExpireSeconds, bean.getSessionid(), rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "wxlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    //用户密码登录
    public RetResult<UserInfo> login(LoginBean bean) {
        final RetResult<UserInfo> result = new RetResult();
        UserInfo user = null;
        boolean unok = true;
        if (bean != null && !bean.emptyCookieinfo() && bean.emptyAccount()) {
            String cookie = decryptAES(bean.getCookieinfo());
            int sharp = cookie.indexOf('#');
            if (sharp > 0) bean.setApptoken(cookie.substring(0, sharp));
            int pos = cookie.indexOf('$');
            int userid = Integer.parseInt(cookie.substring(sharp + 1, pos), 36);
            user = this.findUserInfo(userid);
            if (user != null) {
                char type = cookie.charAt(pos + 1);
                int wen = cookie.indexOf('?');
                String val = wen > 0 ? cookie.substring(pos + 2, wen) : cookie.substring(pos + 2);
                if (type == '0') { //密码
                    bean.setPassword(val);
                } else if (type == '1') { //微信
                    if (!user.getWxunionid().isEmpty()) {
                        unok = !Objects.equals(val, (user.getWxunionid()));
                    }
                } else if (type == '2') { //QQ
                    if (!user.getQqopenid().isEmpty()) {
                        unok = !Objects.equals(val, (user.getQqopenid()));
                    }
                }
            }
        }
        if (bean == null || bean.emptySessionid() || (user == null && bean.emptyAccount())) return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
        String key = "";
        if (user == null && !bean.emptyAccount()) {
            if (bean.getAccount().indexOf('@') > 0) {
                key = "email";
                user = findUserInfoByEmail(bean.getAccount());
            } else if (Character.isDigit(bean.getAccount().charAt(0))) {
                key = "mobile";
                user = findUserInfoByMobile(bean.getAccount());
            } else {
                key = "account";
                user = findUserInfoByAccount(bean.getAccount());
            }
        }
        if (user == null) { //不在缓存内
            UserDetail detail = source.find(UserDetail.class, key, bean.getAccount());
            if (detail == null) return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
            if (bean.getPassword().isEmpty() && !bean.getVercode().isEmpty()) { //手机验证码登录
                RetResult<RandomCode> rr = checkRandomCode(detail.getMobile(), bean.getVercode(), RandomCode.TYPE_SMSLGN);
                if (!rr.isSuccess()) return RetCodes.retResult(rr.getRetcode());
                removeRandomCode(rr.getResult());
            } else if (!detail.getPassword().equals(digestPassword(bean.getPassword()))) {
                return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
            }
            user = detail.createUserInfo();
            if (user.isFrobid()) return RetCodes.retResult(RET_USER_FREEZED);

            result.setRetcode(0);
            result.setResult(user);
            source.insert(user);
        } else { //在缓存内
            if (unok) {
                if (bean.getPassword().isEmpty() && !bean.getVercode().isEmpty()) { //手机验证码登录
                    RetResult<RandomCode> rr = checkRandomCode(user.getMobile(), bean.getVercode(), RandomCode.TYPE_SMSLGN);
                    if (!rr.isSuccess()) return RetCodes.retResult(rr.getRetcode());
                    removeRandomCode(rr.getResult());
                } else if (!user.getPassword().equals(digestPassword(bean.getPassword()))) {
                    return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
                }
            }
            result.setRetcode(0);
            result.setResult(user);
            if (!user.getApptoken().equals(bean.getApptoken())) { //用户设备变更了
                user.setApptoken(bean.getApptoken());
                source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", bean.getApptoken());
                source.updateColumn(UserInfo.class, user.getUserid(), "apptoken", bean.getApptoken());
            }
        }
        this.sessions.set(sessionExpireSeconds, bean.getSessionid(), result.getResult().getUserid());
        return result;
    }

    /**
     *
     * 用户注册
     *
     * @param user
     *
     * @return
     */
    public RetResult<UserInfo> register(UserDetail user) {
        RetResult<UserInfo> result = new RetResult();
        if (user == null) return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
        if (user.getAccount().isEmpty() && user.getMobile().isEmpty()
            && user.getEmail().isEmpty() && user.getWxunionid().isEmpty()
            && user.getQqopenid().isEmpty()) return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
        short gender = user.getGender();
        if (gender != 0 && gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        int retcode = 0;
        if (!user.getAccount().isEmpty() && (retcode = checkAccount(user.getAccount())) != 0) return RetCodes.retResult(retcode);
        if (!user.getMobile().isEmpty() && (retcode = checkMobile(user.getMobile())) != 0) return RetCodes.retResult(retcode);
        if (!user.getEmail().isEmpty() && (retcode = checkEmail(user.getEmail())) != 0) return RetCodes.retResult(retcode);
        if (!user.getWxunionid().isEmpty() && (retcode = checkWxunionid(user.getEmail())) != 0) return RetCodes.retResult(retcode);
        if (!user.getQqopenid().isEmpty() && (retcode = checkQqopenid(user.getEmail())) != 0) return RetCodes.retResult(retcode);
        if (!user.getMobile().isEmpty()) {
            user.setRegtype(REGTYPE_MOBILE);
            if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
        } else if (!user.getEmail().isEmpty()) {
            user.setRegtype(REGTYPE_EMAIL);
            if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
        } else if (!user.getWxunionid().isEmpty()) {
            user.setRegtype(REGTYPE_WEIXIN);
        } else if (!user.getQqopenid().isEmpty()) {
            user.setRegtype(REGTYPE_QQOPEN);
        } else {
            user.setRegtype(REGTYPE_ACCOUNT);
            if (user.getPassword().isEmpty()) return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
        }
        user.setUserid(maxid.incrementAndGet());
        user.setCreatetime(System.currentTimeMillis());
        user.setInfotime(0);
        user.setUpdatetime(0);
        if (!user.getPassword().isEmpty()) {
            user.setPassword(digestPassword(secondPasswordMD5(user.getPassword())));
        }
        user.setStatus(UserInfo.STATUS_NORMAL);
        try {
            source.insert(user);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "register user error (" + user + ")", e);
            if (updateMax()) {
                user.setUserid(maxid.incrementAndGet());
                source.insert(user);
            } else {
                throw e;
            }
        }
        //------------------------扩展信息-----------------------------

        UserInfo info = user.createUserInfo();
        source.insert(info);
        result.setResult(info);
        //可以在此处给企业微信号推送注册消息
        return result;
    }

    //注销登录
    public boolean logout(final String sessionid) {
        UserInfo user = current(sessionid);
        if (user != null && !user.getApptoken().isEmpty()) {
            user.setApptoken("");
            source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", "");
            source.updateColumn(UserInfo.class, user.getUserid(), "apptoken", "");
        }
        sessions.remove(sessionid);
        return true;
    }

    //绑定微信号
    public RetResult updateWxunionid(UserInfo user, String code) {
        try {
            if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
            Map<String, String> wxmap = wxMPService.getMPUserTokenByCode(code);
            final String wxunionid = wxmap.get("unionid");
            if (wxunionid == null || wxunionid.isEmpty()) return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            if (checkWxunionid(wxunionid) != 0) return RetCodes.retResult(RET_USER_WXID_EXISTS);
            source.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", wxunionid);
            source.updateColumn(UserInfo.class, user.getUserid(), "wxunionid", wxunionid);
            user.setWxunionid(wxunionid);
            return RetResult.success();
        } catch (Exception e) {
            logger.log(Level.FINE, "updateWxunionid failed (" + user + ", " + code + ")", e);
            return RetCodes.retResult(RET_USER_WXID_BIND_FAIL);
        }
    }

    public RetResult updateUsername(int userid, String username) {
        if (username == null || username.isEmpty()) return RetCodes.retResult(RET_USER_USERNAME_ILLEGAL);
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (user.getUsername().equals(username)) return RetResult.success();
        if (username.isEmpty()) return RetCodes.retResult(RET_USER_USERNAME_ILLEGAL);
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setUsername(username);
        ud.setInfotime(System.currentTimeMillis());
        source.updateColumns(ud, "username", "infotime");
        user.setUsername(username);
        user.setInfotime(ud.getInfotime());
        source.updateColumns(user, "username", "infotime");
        return RetResult.success();
    }

    public RetResult updateApptoken(int userid, String apptoken) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (apptoken == null) apptoken = "";
        source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", apptoken);
        source.updateColumn(UserInfo.class, user.getUserid(), "apptoken", apptoken);
        user.setApptoken(apptoken);
        return RetResult.success();
    }

    public RetResult updateInfotime(int userid) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "infotime", t);
        source.updateColumn(UserInfo.class, user.getUserid(), "infotime", t);
        user.setInfotime(t);
        return RetResult.success();
    }

    public RetResult updateGender(int userid, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        source.updateColumn(UserDetail.class, user.getUserid(), "gender", gender);
        source.updateColumn(UserInfo.class, user.getUserid(), "gender", gender);
        user.setGender(gender);
        return RetResult.success();
    }

    //precode 表示原手机号码收到的短信验证码，如果当前用户没有配置手机号码，则该值忽略
    public RetResult updateMobile(int userid, String newmobile, String vercode, String precode) {
        int retcode = checkMobile(newmobile);
        if (retcode != 0) return RetCodes.retResult(retcode);
        RandomCode code = source.find(RandomCode.class, newmobile + "-" + vercode);
        if (code == null) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);

        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);
        RandomCode rc = null;
        if (!user.getMobile().isEmpty()) {
            rc = source.find(RandomCode.class, user.getMobile() + "-" + precode);
            if (rc == null) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
            if (rc.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
        }
        source.updateColumn(UserDetail.class, user.getUserid(), "mobile", newmobile);
        source.updateColumn(UserInfo.class, user.getUserid(), "mobile", newmobile);
        user.setMobile(newmobile);
        code.setUserid(user.getUserid());
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
        if (rc != null) {
            source.insert(rc.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
            source.delete(RandomCode.class, rc.getRandomcode());
        }
        return RetResult.success();
    }

    public RetResult<UserInfo> updatePwd(UserPwdBean bean) {
        UserInfo user = bean.getSessionid() == null ? null : current(bean.getSessionid());
        final String newpwd = digestPassword(secondPasswordMD5(bean.getNewpwd())); //HEX-MD5(密码明文)
        if (user == null) {  //表示忘记密码后进行重置密码
            bean.setSessionid(null);
            String randomcode = bean.getRandomcode();
            if (randomcode == null || randomcode.isEmpty()) {
                if (bean.getAccount() != null && !bean.getAccount().isEmpty()
                    && bean.getVercode() != null && !bean.getVercode().isEmpty()) {
                    randomcode = bean.getAccount() + "-" + bean.getVercode();
                }
            }
            if (randomcode != null && !randomcode.isEmpty()) {
                RandomCode code = source.find(RandomCode.class, randomcode);
                if (code == null || code.getType() != RandomCode.TYPE_SMSPWD) return retResult(RET_USER_RANDCODE_ILLEGAL);
                if (code.isExpired()) return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);

                user = findUserInfo(code.getUserid());
                if (user == null) return RetCodes.retResult(RET_USER_NOTEXISTS);

                source.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
                source.updateColumn(UserInfo.class, user.getUserid(), "password", newpwd);
                user.setPassword(newpwd);
                source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
                source.delete(RandomCode.class, code.getRandomcode());
                return new RetResult<>(user);
            }
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        //用户或密码错误
        if (!Objects.equals(user.getPassword(), digestPassword(secondPasswordMD5(bean.getOldpwd())))) {
            return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);  //原密码错误
        }
        source.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
        source.updateColumn(UserInfo.class, user.getUserid(), "password", newpwd);
        user.setPassword(newpwd);
        return new RetResult<>(user);
    }

    protected UserDetail findUserDetail(int userid) {
        return source.find(UserDetail.class, userid);
    }

    public RetResult<RandomCode> checkRandomCode(String targetid, String randomcode, short type) {
        if (randomcode == null || randomcode.isEmpty()) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) randomcode = targetid + "-" + randomcode;
        RandomCode code = source.find(RandomCode.class, randomcode);
        if (code != null && type > 0 && code.getType() != type) return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        return code == null ? RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL) : (code.isExpired() ? RetCodes.retResult(RET_USER_RANDCODE_EXPIRED) : new RetResult(code));
    }

    public void removeRandomCode(RandomCode code) {
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
    }

    private static final Predicate<String> accountReg = Pattern.compile("^[a-zA-Z][\\w_.]{6,64}$").asPredicate();

    /**
     * 检测账号是否有效, 返回0表示手机号码可用
     * 账号不能以数字开头、不能包含@ ， 用于区分手机号码和邮箱
     *
     * @param account
     *
     * @return
     */
    public int checkAccount(String account) {
        if (account == null) return RET_USER_ACCOUNT_ILLEGAL;
        if (!accountReg.test(account)) return RET_USER_ACCOUNT_ILLEGAL;
        return source.exists(UserInfo.class, FilterNode.create("account", IGNORECASEEQUAL, account)) ? RET_USER_ACCOUNT_EXISTS : 0;
    }

    private static final Predicate<String> mobileReg = Pattern.compile("^\\d{6,18}$").asPredicate();

    /**
     * 检测手机号码是否有效, 返回0表示手机号码可用
     *
     * @param mobile
     *
     * @return
     */
    public int checkMobile(String mobile) {
        if (mobile == null) return RET_USER_MOBILE_ILLEGAL;
        if (!mobileReg.test(mobile)) return RET_USER_MOBILE_ILLEGAL;
        return source.exists(UserInfo.class, FilterNode.create("mobile", EQUAL, mobile)) ? RET_USER_MOBILE_EXISTS : 0;
    }

    private static final Predicate<String> emailReg = Pattern.compile("^(\\w|\\.|-)+@(\\w|-)+(\\.(\\w|-)+)+$").asPredicate();

    /**
     * 检测邮箱地址是否有效, 返回0表示邮箱地址可用.给新用户注册使用
     *
     * @param email
     *
     * @return
     */
    public int checkEmail(String email) {
        if (email == null) return RET_USER_EMAIL_ILLEGAL;
        if (!emailReg.test(email)) return RET_USER_EMAIL_ILLEGAL;
        return source.exists(UserInfo.class, FilterNode.create("email", IGNORECASEEQUAL, email)) ? RET_USER_EMAIL_EXISTS : 0;
    }

    public int checkWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) return 0;
        return source.exists(UserInfo.class, FilterNode.create("wxunionid", EQUAL, wxunionid)) ? RET_USER_WXID_EXISTS : 0;
    }

    public int checkQqopenid(String qqopenid) {
        if (qqopenid == null || qqopenid.isEmpty()) return 0;
        return source.exists(UserInfo.class, FilterNode.create("qqopenid", EQUAL, qqopenid)) ? RET_USER_QQID_EXISTS : 0;
    }

    //AES加密
    public static String encryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            synchronized (aesEncrypter) {
                return Utility.binToHexString(aesEncrypter.doFinal(value.getBytes()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //AES解密
    public static String decryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        byte[] hex = Utility.hexToBin(value);
        try {
            synchronized (aesEncrypter) {
                return new String(aesDecrypter.doFinal(hex));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //第二次MD5
    public static String secondPasswordMD5(String passwordoncemd5) {
        byte[] bytes = ("REDKALE-" + passwordoncemd5.trim().toLowerCase()).getBytes();
        synchronized (md5) {
            bytes = md5.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

    //第三次密码加密
    public static String digestPassword(String passwordtwicemd5) {
        if (passwordtwicemd5 == null || passwordtwicemd5.isEmpty()) return passwordtwicemd5;
        byte[] bytes = (passwordtwicemd5.trim().toLowerCase() + "-REDKALE").getBytes();
        synchronized (sha1) {
            bytes = sha1.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

}
