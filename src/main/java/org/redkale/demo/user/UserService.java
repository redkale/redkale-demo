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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import org.redkale.annotation.*;
import org.redkale.annotation.Comment;
import org.redkale.convert.json.JsonConvert;
import org.redkale.demo.base.*;
import static org.redkale.demo.base.RetCodes.*;
import static org.redkale.demo.base.UserInfo.*;
import org.redkale.demo.file.FileService;
import org.redkale.demo.info.MobileGroupService;
import org.redkale.demo.notice.*;
import static org.redkale.demo.user.UserDetail.*;
import org.redkale.service.RetResult;
import org.redkale.source.*;
import org.redkale.util.*;
import org.redkalex.weixin.WeiXinMPService;

/**
 *
 * @author zhangjx
 */
@Comment("用户服务模块")
public class UserService extends BaseService {

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

    private final int sessionExpireSeconds = 30 * 60;

    @Resource(name = "usersessions")
    protected CacheSource sessions;

    @Resource
    protected FileService fileService;

    @Resource
    private WeiXinMPService wxMPService;

    @Resource
    private SmsService smsService;

    @Resource
    private JsonConvert convert;

    @Resource
    private RandomService randomCodeService;

    @Resource
    private MobileGroupService mobileGroupService;

    @Override
    public void init(AnyValue conf) {

    }

    @Override
    public void destroy(AnyValue conf) {
    }

    public UserInfo findUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) {
            return UserInfo.USER_SYSTEM;
        }
        return source.find(UserInfo.class, userid);
    }

    @Comment("根据账号查找用户")
    public UserInfo findUserInfoByAccount(String account) {
        if (account == null || account.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.igEq("account", account));
    }

    @Comment("根据手机号码查找用户")
    public UserInfo findUserInfoByMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.eq("mobile", mobile));
    }

    @Comment("根据邮箱地址查找用户")
    public UserInfo findUserInfoByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.igEq("email", email));
    }

    @Comment("根据微信绑定ID查找用户")
    public UserInfo findUserInfoByWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.eq("wxunionid", wxunionid));
    }

    @Comment("根据QQ绑定ID查找用户")
    public UserInfo findUserInfoByQqopenid(String qqopenid) {
        if (qqopenid == null || qqopenid.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.eq("qqopenid", qqopenid));
    }

    @Comment("根据APP设备ID查找用户")
    public UserInfo findUserInfoByAppToken(String appToken) {
        if (appToken == null || appToken.isEmpty()) {
            return null;
        }
        return source.find(UserInfo.class, FilterNodes.eq("appToken", appToken));
    }

    @Comment("查询用户列表， 通常用于后台管理系统查询")
    public Sheet<UserDetail> queryUserDetail(FilterNode node, Flipper flipper) {
        return source.querySheet(UserDetail.class, flipper, node);
    }

    @Comment("根据登录态获取当前用户信息")
    public UserInfo current(String sessionid) {
        long userid = sessions.getexLong(sessionid, sessionExpireSeconds, 0L);
        return userid == 0 ? null : findUserInfo((int) userid);
    }

    @Comment("发送短信验证码")
    public RetResult smscode(final short type, String mobile) {
        if (mobile == null) {
            return new RetResult(RET_USER_MOBILE_ILLEGAL, type + " mobile is null"); //手机号码无效
        }
        if (mobile.indexOf('+') == 0) {
            mobile = mobile.substring(1);
        }
        UserInfo info = findUserInfoByMobile(mobile);
        if (type == RandomCode.TYPE_SMSREG || type == RandomCode.TYPE_SMSMOB) { //手机注册或手机修改的号码不能已存在
            if (info != null) {
                return new RetResult(RET_USER_MOBILE_EXISTS, "smsreg or smsmob mobile " + mobile + " exists");
            }
        } else if (type == RandomCode.TYPE_SMSPWD) { //修改密码
            if (info == null) {
                return new RetResult(RET_USER_NOTEXISTS, "smspwd mobile " + mobile + " not exists");
            }
        } else if (type == RandomCode.TYPE_SMSLGN) { //手机登录
            if (info == null) {
                return new RetResult(RET_USER_NOTEXISTS, "smslgn mobile " + mobile + " not exists");
            }
        } else if (type == RandomCode.TYPE_SMSODM) { //原手机
            if (info == null) {
                return new RetResult(RET_USER_NOTEXISTS, "smsodm mobile " + mobile + " not exists");
            }
        } else {
            return new RetResult(RET_PARAMS_ILLEGAL, type + " is illegal");
        }
        List<RandomCode> codes = randomCodeService.queryRandomCodeByMobile(mobile);
        if (!codes.isEmpty()) {
            RandomCode last = codes.get(codes.size() - 1);
            if (last.getCreateTime() + 60 * 1000 > System.currentTimeMillis()) {
                return RetCodes.retResult(RET_USER_MOBILE_SMSFREQUENT);
            }
        }
        final int smscode = RandomCode.randomSmsCode();
        try {
            if (!smsService.sendRandomSmsCode(type, mobile, smscode)) {
                return retResult(RET_USER_MOBILE_SMSFREQUENT);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "mobile(" + mobile + ", type=" + type + ") send smscode " + smscode + " error", e);
            return retResult(RET_USER_MOBILE_SMSFREQUENT);
        }
        RandomCode code = new RandomCode();
        code.setCreateTime(System.currentTimeMillis());
        if (info != null) {
            code.setUserid(info.getUserid());
        }
        code.setRandomcode(mobile + "-" + smscode);
        code.setType(type);
        randomCodeService.createRandomCode(code);
        return RetResult.success();
    }

    @Comment("QQ登录")
    public RetResult<UserInfo> qqlogin(LoginQQBean bean) {
        try {
            String qqappid = "xxxx";
            String url = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + qqappid + "&access_token=" + bean.getAccessToken() + "&openid=" + bean.getOpenid() + "&format=json";
            String json = Utility.getHttpContent(url);
            if (finest) {
                logger.finest(url + "--->" + json);
            }
            Map<String, String> jsonmap = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, json);
            if (!"0".equals(jsonmap.get("ret"))) {
                return RetCodes.retResult(RET_USER_QQID_INFO_FAIL);
            }
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByQqopenid(bean.getOpenid());
            if (user == null) {
                UserDetail detail = new UserDetail();
                detail.setUserName(jsonmap.getOrDefault("nickname", "qq-user"));
                detail.setQqopenid(bean.getOpenid());
                detail.setRegAgent(bean.getLoginAgent());
                detail.setRegAddr(bean.getLoginAddr());
                detail.setAppos(bean.getAppos());
                detail.setAppToken(bean.getAppToken());
                detail.setRegType(REGTYPE_QQOPEN);
                String genstr = jsonmap.getOrDefault("gender", "");
                detail.setGender("男".equals(genstr) ? UserInfo.GENDER_MALE : ("女".equals(genstr) ? UserInfo.GENDER_FEMALE : (short) 0));
                if (finer) {
                    logger.fine(bean + " --qqlogin-->" + convert.convertTo(jsonmap));
                }
                rr = register(detail);
                if (rr.isSuccess()) {
                    rr.setRetinfo(jsonmap.get(bean.getOpenid()));
                    String headimgurl = jsonmap.get("figureurl_qq_2");
                    if (headimgurl != null) {
                        super.runAsync(() -> {
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
                this.sessions.setexLong(bean.getSessionid(), sessionExpireSeconds, rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "qqlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    @Comment("微信登录")
    public RetResult<UserInfo> wxlogin(LoginWXBean bean) {
        try {
            Map<String, String> wxmap = bean.emptyAccessToken()
                ? wxMPService.getMPUserTokenByCode(bean.getCode()).join()
                : wxMPService.getMPUserTokenByOpenid(bean.getAccessToken(), bean.getOpenid()).join();
            final String unionid = wxmap.get("unionid");
            if (unionid == null) {
                return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            }
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByWxunionid(unionid);
            if (user == null) {
                if (!bean.isAutoreg()) {
                    return new RetResult(0, convert.convertTo(wxmap));
                }
                UserDetail detail = new UserDetail();
                detail.setUserName(wxmap.getOrDefault("nickname", "wx-user"));
                detail.setWxunionid(unionid);
                detail.setAppos(bean.getAppos());
                detail.setAppToken(bean.getAppToken());
                detail.setRegType(REGTYPE_WEIXIN);
                detail.setRegAgent(bean.getLoginAgent());
                detail.setRegAddr(bean.getLoginAddr());
                detail.setGender((short) (Short.parseShort(wxmap.getOrDefault("sex", "0")) * 2));
                logger.fine(bean + " --wxlogin-->" + convert.convertTo(wxmap));
                rr = register(detail);
                if (rr.isSuccess()) {
                    rr.setRetinfo(wxmap.get("openid"));
                    String headimgurl = wxmap.get("headimgurl");
                    if (headimgurl != null) {
                        super.runAsync(() -> {
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
                if (!user.getAppToken().equals(bean.getAppToken())) {
                    user.setAppToken(bean.getAppToken());
                    source.updateColumn(UserDetail.class, user.getUserid(), "appToken", bean.getAppToken());
                    source.updateColumn(UserInfo.class, user.getUserid(), "appToken", bean.getAppToken());
                }
            }
            if (rr.isSuccess()) {
                this.sessions.setexLong(bean.getSessionid(), sessionExpireSeconds, rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "wxlogin failed (" + bean + ")", e);
            return RetCodes.retResult(RET_USER_LOGIN_FAIL);
        }
    }

    @Comment("用户密码登录")
    public RetResult<UserInfo> login(LoginBean bean) {
        final RetResult<UserInfo> result = new RetResult();
        UserInfo user = null;
        boolean unok = true;
        if (bean != null && !bean.emptyCookieInfo() && bean.emptyAccount()) {
            String cookie;
            try {
                cookie = decryptAES(bean.getCookieInfo());
            } catch (Exception e) {
                return retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
            }
            int sharp = cookie.indexOf('#');
            if (sharp > 0) {
                bean.setAppToken(cookie.substring(0, sharp));
            }
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
        if (bean == null || bean.emptySessionid() || (user == null && bean.emptyAccount())) {
            return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
        }
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
            if (detail == null) {
                return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL);
            }
            if (bean.getPassword().isEmpty() && !bean.getVerCode().isEmpty()) { //手机验证码登录
                RetResult<RandomCode> rr = checkRandomCode(detail.getMobile(), bean.getVerCode(), RandomCode.TYPE_SMSLGN);
                if (!rr.isSuccess()) {
                    return RetCodes.retResult(rr.getRetcode());
                }
                removeRandomCode(rr.getResult());
            } else if (!detail.getPassword().equals(digestPassword(bean.getPassword()))) {
                return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
            }
            user = detail.createUserInfo();
            if (user.isFrobid()) {
                return RetCodes.retResult(RET_USER_FREEZED);
            }

            result.setRetcode(0);
            result.setResult(user);
            source.insert(user);
        } else { //在缓存内
            if (unok) {
                if (bean.getPassword().isEmpty() && !bean.getVerCode().isEmpty()) { //手机验证码登录
                    RetResult<RandomCode> rr = checkRandomCode(user.getMobile(), bean.getVerCode(), RandomCode.TYPE_SMSLGN);
                    if (!rr.isSuccess()) {
                        return RetCodes.retResult(rr.getRetcode());
                    }
                    removeRandomCode(rr.getResult());
                } else if (!user.getPassword().equals(digestPassword(bean.getPassword()))) {
                    return RetCodes.retResult(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
                }
            }
            if (user.isFrobid()) {
                return RetCodes.retResult(RET_USER_FREEZED);
            }
            result.setRetcode(0);
            result.setResult(user);
            if (!user.getAppToken().equals(bean.getAppToken())) { //用户设备变更了
                user.setAppos(bean.getAppos());
                user.setAppToken(bean.getAppToken());
                source.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("appToken", bean.getAppToken()));
                source.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", bean.getAppos()), ColumnValue.mov("appToken", bean.getAppToken()));
            }
        }
        this.sessions.setexLong(bean.getSessionid(), sessionExpireSeconds, result.getResult().getUserid());
        return result;
    }

    @Comment("用户注册")
    public RetResult<UserInfo> register(UserDetail user) {
        RetResult<UserInfo> result = new RetResult();
        if (user == null) {
            return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
        }
        if (user.getAccount().isEmpty() && user.getMobile().isEmpty()
            && user.getEmail().isEmpty() && user.getWxunionid().isEmpty()
            && user.getQqopenid().isEmpty()) {
            return RetCodes.retResult(RET_USER_SIGNUP_ILLEGAL);
        }
        short gender = user.getGender();
        if (gender != 0 && gender != GENDER_MALE && gender != GENDER_FEMALE) {
            return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        }
        int retcode = 0;
        if (!user.getAccount().isEmpty() && (retcode = checkAccount(user.getAccount())) != 0) {
            return RetCodes.retResult(retcode);
        }
        if (!user.getMobile().isEmpty() && (retcode = checkMobile(user.getMobile())) != 0) {
            return RetCodes.retResult(retcode);
        }
        if (!user.getEmail().isEmpty() && (retcode = checkEmail(user.getEmail())) != 0) {
            return RetCodes.retResult(retcode);
        }
        if (!user.getWxunionid().isEmpty() && (retcode = checkWxunionid(user.getEmail())) != 0) {
            return RetCodes.retResult(retcode);
        }
        if (!user.getQqopenid().isEmpty() && (retcode = checkQqopenid(user.getEmail())) != 0) {
            return RetCodes.retResult(retcode);
        }
        if (!user.getMobile().isEmpty()) {
            user.setRegType(REGTYPE_MOBILE);
            if (user.getPassword().isEmpty()) {
                return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            }
        } else if (!user.getEmail().isEmpty()) {
            user.setRegType(REGTYPE_EMAIL);
            if (user.getPassword().isEmpty()) {
                return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            }
        } else if (!user.getWxunionid().isEmpty()) {
            user.setRegType(REGTYPE_WEIXIN);
        } else if (!user.getQqopenid().isEmpty()) {
            user.setRegType(REGTYPE_QQOPEN);
        } else {
            user.setRegType(REGTYPE_ACCOUNT);
            if (user.getPassword().isEmpty()) {
                return RetCodes.retResult(RET_USER_PASSWORD_ILLEGAL);
            }
        }
        user.setCreateTime(System.currentTimeMillis());
        user.setUpdateTime(0);
        if (!user.getPassword().isEmpty()) {
            user.setPassword(digestPassword(secondPasswordMD5(user.getPassword())));
        }
        user.setStatus(UserInfo.STATUS_NORMAL);
        int maxid = source.getNumberResult(UserDetail.class, FilterFunc.MAX, 2_0000_0000, "userid", (FilterNode) null).intValue();
        boolean ok = false;
        for (int i = 0; i < 20; i++) {
            try {
                user.setUserid(maxid + 1);
                source.insert(user);
                ok = true;
                break;
            } catch (Exception e) { //并发时可能会重复创建， 忽略异常
                logger.log(Level.INFO, "create userdetail error: " + user, e);
                maxid = source.getNumberResult(UserDetail.class, FilterFunc.MAX, 2_0000_0000, "userid", (FilterNode) null).intValue();
            }
        }
        if (!ok) {
            return RetCodes.retResult(RET_PARAMS_ILLEGAL);
        }

        //------------------------扩展信息-----------------------------
        UserInfo info = user.createUserInfo();
        source.insert(info);
        result.setResult(info);
        //可以在此处给企业微信号推送注册消息
        return result;
    }

    @Comment("注销登录")
    public boolean logout(final String sessionid) {
        UserInfo user = current(sessionid);
        if (user != null && !user.getAppToken().isEmpty()) {
            user.setAppos("");
            user.setAppToken("");
            source.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("appos", ""), ColumnValue.mov("appToken", ""));
            source.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", ""), ColumnValue.mov("appToken", ""));
        }
        sessions.del(sessionid);
        return true;
    }

    @Comment("绑定微信号")
    public RetResult updateWxunionid(UserInfo user, String code) {
        try {
            if (user == null) {
                return RetCodes.retResult(RET_USER_NOTEXISTS);
            }
            Map<String, String> wxmap = wxMPService.getMPUserTokenByCode(code).join();
            final String wxunionid = wxmap.get("unionid");
            if (wxunionid == null || wxunionid.isEmpty()) {
                return RetCodes.retResult(RET_USER_WXID_ILLEGAL);
            }
            if (checkWxunionid(wxunionid) != 0) {
                return RetCodes.retResult(RET_USER_WXID_EXISTS);
            }
            source.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", wxunionid);
            source.updateColumn(UserInfo.class, user.getUserid(), "wxunionid", wxunionid);
            user.setWxunionid(wxunionid);
            return RetResult.success();
        } catch (Exception e) {
            logger.log(Level.FINE, "updateWxunionid failed (" + user + ", " + code + ")", e);
            return RetCodes.retResult(RET_USER_WXID_BIND_FAIL);
        }
    }

    public RetResult updateUserName(int userid, String userName) {
        if (userName == null || userName.isEmpty()) {
            return RetCodes.retResult(RET_USER_USERNAME_ILLEGAL);
        }
        UserInfo user = findUserInfo(userid);
        if (user == null) {
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        if (user.getUserName().equals(userName)) {
            return RetResult.success();
        }
        if (userName.isEmpty()) {
            return RetCodes.retResult(RET_USER_USERNAME_ILLEGAL);
        }
        UserDetail ud = new UserDetail();
        ud.setUserid(userid);
        ud.setUserName(userName);
        source.updateColumn(ud, "userName");
        user.setUserName(userName);
        source.updateColumn(user, "userName");
        return RetResult.success();
    }

    public RetResult updateAppToken(int userid, String appos, String appToken) {
        UserInfo user = findUserInfo(userid);
        if (user == null) {
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        if (appos == null) {
            appos = "";
        }
        if (appToken == null) {
            appToken = "";
        }
        source.updateColumn(UserDetail.class, user.getUserid(), ColumnValue.mov("appos", appos.toLowerCase()), ColumnValue.mov("appToken", appToken));
        source.updateColumn(UserInfo.class, user.getUserid(), ColumnValue.mov("appos", appos.toLowerCase()), ColumnValue.mov("appToken", appToken));
        user.setAppos(appos.toLowerCase());
        user.setAppToken(appToken);
        return RetResult.success();
    }

    public RetResult updateGender(int userid, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) {
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) {
            return RetCodes.retResult(RET_USER_GENDER_ILLEGAL);
        }
        source.updateColumn(UserDetail.class, user.getUserid(), "gender", gender);
        source.updateColumn(UserInfo.class, user.getUserid(), "gender", gender);
        user.setGender(gender);
        return RetResult.success();
    }

    //precode 表示原手机号码收到的短信验证码，如果当前用户没有配置手机号码，则该值忽略
    public RetResult updateMobile(int userid, String newmobile, String verCode, String precode) {
        int retcode = checkMobile(newmobile);
        if (retcode != 0) {
            return RetCodes.retResult(retcode);
        }
        RandomCode code = source.find(RandomCode.class, newmobile + "-" + verCode);
        if (code == null) {
            return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        }
        if (code.isExpired()) {
            return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
        }

        UserInfo user = findUserInfo(userid);
        if (user == null) {
            return RetCodes.retResult(RET_USER_NOTEXISTS);
        }
        RandomCode rc = null;
        if (!user.getMobile().isEmpty()) {
            rc = source.find(RandomCode.class, user.getMobile() + "-" + precode);
            if (rc == null) {
                return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
            }
            if (rc.isExpired()) {
                return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
            }
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
                    && bean.getVerCode() != null && !bean.getVerCode().isEmpty()) {
                    randomcode = bean.getAccount() + "-" + bean.getVerCode();
                }
            }
            if (randomcode != null && !randomcode.isEmpty()) {
                RandomCode code = source.find(RandomCode.class, randomcode);
                if (code == null || code.getType() != RandomCode.TYPE_SMSPWD) {
                    return retResult(RET_USER_RANDCODE_ILLEGAL);
                }
                if (code.isExpired()) {
                    return RetCodes.retResult(RET_USER_RANDCODE_EXPIRED);
                }

                user = findUserInfo((int) code.getUserid());
                if (user == null) {
                    return RetCodes.retResult(RET_USER_NOTEXISTS);
                }

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
        if (randomcode == null || randomcode.isEmpty()) {
            return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        }
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) {
            randomcode = targetid + "-" + randomcode;
        }
        RandomCode code = source.find(RandomCode.class, randomcode);
        if (code != null && type > 0 && code.getType() != type) {
            return RetCodes.retResult(RET_USER_RANDCODE_ILLEGAL);
        }
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
        if (account == null) {
            return RET_USER_ACCOUNT_ILLEGAL;
        }
        if (!accountReg.test(account)) {
            return RET_USER_ACCOUNT_ILLEGAL;
        }
        return source.exists(UserInfo.class, FilterNodes.igEq("account", account)) ? RET_USER_ACCOUNT_EXISTS : 0;
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
        if (mobile == null) {
            return RET_USER_MOBILE_ILLEGAL;
        }
        if (!mobileReg.test(mobile)) {
            return RET_USER_MOBILE_ILLEGAL;
        }
        return source.exists(UserInfo.class, FilterNodes.eq("mobile", mobile)) ? RET_USER_MOBILE_EXISTS : 0;
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
        if (email == null) {
            return RET_USER_EMAIL_ILLEGAL;
        }
        if (!emailReg.test(email)) {
            return RET_USER_EMAIL_ILLEGAL;
        }
        return source.exists(UserInfo.class, FilterNodes.igEq("email", email)) ? RET_USER_EMAIL_EXISTS : 0;
    }

    public int checkWxunionid(String wxunionid) {
        if (wxunionid == null || wxunionid.isEmpty()) {
            return 0;
        }
        return source.exists(UserInfo.class, FilterNodes.eq("wxunionid", wxunionid)) ? RET_USER_WXID_EXISTS : 0;
    }

    public int checkQqopenid(String qqopenid) {
        if (qqopenid == null || qqopenid.isEmpty()) {
            return 0;
        }
        return source.exists(UserInfo.class, FilterNodes.eq("qqopenid", qqopenid)) ? RET_USER_QQID_EXISTS : 0;
    }

    //AES加密
    public static String encryptAES(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            synchronized (aesEncrypter) {
                return Utility.binToHexString(aesEncrypter.doFinal(value.getBytes()));
            }
        } catch (Exception e) {
            throw new RedkaleException(e);
        }
    }

    //AES解密
    public static String decryptAES(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
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
        if (passwordtwicemd5 == null || passwordtwicemd5.isEmpty()) {
            return passwordtwicemd5;
        }
        byte[] bytes = (passwordtwicemd5.trim().toLowerCase() + "-REDKALE").getBytes();
        synchronized (sha1) {
            bytes = sha1.digest(bytes);
        }
        return new String(Utility.binToHex(bytes));
    }

}
