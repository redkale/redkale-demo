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
import java.util.concurrent.*;
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
import static org.redkale.demo.base.RetCodes.*;
import static org.redkale.demo.base.UserInfo.*;
import org.redkale.demo.file.FileService;
import static org.redkale.demo.user.UserDetail.*;
import org.redkale.plugins.email.EmailService;
import org.redkale.plugins.weixin.WeiXinMPService;
import org.redkale.service.*;
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
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

    protected final Map<Integer, UserInfo> userInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> accountUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> mobileUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> emailUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> wxunionidUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> qqopenidUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> apptokenUserInfos = new ConcurrentHashMap<>();

    protected final AtomicInteger maxid = new AtomicInteger(200000000);

    @Resource(name = "reduser")
    private DataSource source;

    private int sessionExpireSeconds = 30 * 60;

    @Resource(name = "usersessions")
    protected CacheSource<String, Integer> sessions;

    @Resource
    protected FileService fileService;

    @Resource
    private WeiXinMPService wxMPService;

    @Resource
    private EmailService emailService;

    @Resource
    private JsonConvert convert;

    private final String userbundle = this.getClass().getPackage().getName() + ".userbundle";

    @Override
    public void init(AnyValue conf) {
        updateMax();
        //---------------------------------------------
        final Flipper flipper = new Flipper(10000);
        flipper.setPage(0);
        final long s = System.currentTimeMillis();
        final AtomicBoolean flag = new AtomicBoolean(true);
        final int count = 10;
        final CountDownLatch cdl = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            super.submit(() -> {
                try {
                    Flipper f;
                    while (flag.get()) {
                        synchronized (flipper) {
                            f = flipper.next().clone();
                        }
                        Sheet<UserDetail> sheet = source.querySheet(UserDetail.class, f, (FilterBean) null);
                        if (sheet.isEmpty()) {
                            flag.set(false);
                            break;
                        }
                        if (sheet.getRows().size() < f.getSize()) flag.set(false);
                        for (UserDetail detail : sheet.getRows()) {
                            UserInfo info = detail.createUserInfo();
                            this.putUserInfo(info, false);
                        }
                    }
                } finally {
                    cdl.countDown();
                }
            });
        }
        try {
            cdl.await();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "load userdetail error", e);
        }
        logger.info("load userdetail count is " + userInfos.size() + ", cost " + (System.currentTimeMillis() - s) + " ms");

    }

    private boolean updateMax() {
        boolean rs = false;
        Number max = source.getNumberResult(UserDetail.class, FilterFunc.MAX, "userid");
        if (max != null && max.intValue() > 200000000) maxid.set(max.intValue());
        rs |= max != null;
        return rs;
    }

    @Override
    public void destroy(AnyValue conf) {
    }

    //根据用户ID查找用户
    public UserInfo findUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM;
        UserInfo info = userInfos.get(userid);
        if (info != null) return info;
        UserDetail detail = source.find(UserDetail.class, userid);
        info = detail == null ? null : detail.createUserInfo();
        if (info != null) updateUserInfo(info, false);
        return info;
    }

    //根据账号查找用户
    public UserInfo findUserInfoByAccount(String account) {
        return account == null || account.isEmpty() ? null : this.accountUserInfos.get(account.toLowerCase());
    }

    //根据手机号码查找用户
    public UserInfo findUserInfoByMobile(String mobile) {
        return mobile == null || mobile.isEmpty() ? null : this.mobileUserInfos.get(mobile);
    }

    //根据邮箱地址查找用户
    public UserInfo findUserInfoByEmail(String email) {
        return email == null || email.isEmpty() ? null : this.emailUserInfos.get(email.toLowerCase());
    }

    //根据微信绑定ID查找用户
    public UserInfo findUserInfoByWxunionid(String wxunionid) {
        return wxunionid == null || wxunionid.isEmpty() ? null : this.wxunionidUserInfos.get(wxunionid);
    }

    //根据QQ绑定ID查找用户
    public UserInfo findUserInfoByQqopenid(String qqopenid) {
        return qqopenid == null || qqopenid.isEmpty() ? null : this.qqopenidUserInfos.get(qqopenid);
    }

    //根据APP设备ID查找用户
    public UserInfo findUserInfoByApptoken(String apptoken) {
        return apptoken == null || apptoken.isEmpty() ? null : this.apptokenUserInfos.get(apptoken);
    }

    //查询用户列表， 通常用于后台管理系统查询
    public Sheet<UserDetail> queryUserDetail(FilterNode node, Flipper flipper) {
        return source.querySheet(UserDetail.class, flipper, node);
    }

    //更新缓存信息并同步到其他等节点服务
    @MultiRun
    public void updateUserInfo(UserInfo info, final boolean replace) {
        if (info == null) return;
        if (info.getUserid() > maxid.get()) maxid.set(info.getUserid());
        putUserInfo(info, replace);
        if (finer) logger.finer("updateUserInfo userinfo (" + info + ")");
    }

    private void putUserInfo(UserInfo info, final boolean replace) {
        UserInfo old = userInfos.get(info.getUserid());
        if (replace) {
            if (old != null) {
                if (old.isAc()) accountUserInfos.remove(old.getAccount());
                if (old.isMb()) mobileUserInfos.remove(old.getMobile());
                if (old.isEm()) emailUserInfos.remove(old.getEmail().toLowerCase());
                if (old.isWx()) wxunionidUserInfos.remove(old.getWxunionid());
                if (old.isQq()) qqopenidUserInfos.remove(old.getQqopenid());
                if (old.isAp()) apptokenUserInfos.remove(old.getApptoken());
            }
        }
        if (old != null) info = info.copyTo(old);
        userInfos.put(info.getUserid(), info);
        if (info.isAc()) accountUserInfos.put(old.getAccount().toLowerCase(), info);
        if (info.isEm()) emailUserInfos.put(info.getEmail().toLowerCase(), info);
        if (info.isMb()) mobileUserInfos.put(info.getMobile(), info);
        if (info.isWx()) wxunionidUserInfos.put(info.getWxunionid(), info);
        if (info.isQq()) qqopenidUserInfos.put(info.getQqopenid(), info);
        if (info.isAp()) apptokenUserInfos.put(info.getApptoken(), info);
    }

    //根据登录态获取当前用户信息
    public UserInfo current(String sessionid) {
        Integer userid = sessions.getAndRefresh(sessionid, sessionExpireSeconds);
        return userid == null ? null : userInfos.get(userid);
    }

    //绑定微信号
    public RetResult updateWxunionid(UserInfo user, String appid, String code) {
        try {
            if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);
            Map<String, String> wxmap = wxMPService.getMPUserTokenByCode(appid, code);
            final String wxunionid = wxmap.get("unionid");
            if (wxunionid == null || wxunionid.isEmpty()) return RetCodes.create(RET_USER_WXID_ILLEGAL);
            if (!checkWxunionid(wxunionid)) return RetCodes.create(RET_USER_WXID_EXISTS);
            user = user.copy();
            source.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", wxunionid);
            user.setWxunionid(wxunionid);
            updateUserInfo(user, true);
            return new RetResult();
        } catch (Exception e) {
            logger.log(Level.FINE, "updateWxunionid failed (" + user + ", " + appid + ", " + code + ")", e);
            return RetCodes.create(RET_USER_WXID_BIND_FAIL);
        }
    }

    //QQ登录
    public RetResult<UserInfo> qqlogin(LoginQQBean bean) {
        try {
            String qqappid = "xxxx";
            String url = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + qqappid + "&access_token=" + bean.getAccesstoken() + "&openid=" + bean.getOpenid() + "&format=json";
            String json = Utility.getHttpContent(url);
            if (finest) logger.finest(url + "--->" + json);
            Map<String, String> jsonmap = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, json);
            if (!"0".equals(jsonmap.get("ret"))) return RetCodes.create(RET_USER_QQID_INFO_FAIL);
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
                                fileService.storeFace(rr.getResult().getUserid(), image.getType(), image);
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
            return RetCodes.create(RET_USER_LOGIN_FAIL);
        }
    }

    //微信登陆
    public RetResult<UserInfo> wxlogin(LoginWXBean bean) {
        try {
            Map<String, String> wxmap = bean.emptyAccesstoken()
                ? wxMPService.getMPUserTokenByCode(bean.getAppid(), bean.getCode())
                : wxMPService.getMPUserTokenByOpenid(bean.getAccesstoken(), bean.getOpenid());
            final String unionid = wxmap.get("unionid");
            if (unionid == null) return RetCodes.create(RET_USER_WXID_ILLEGAL);
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
                                fileService.storeFace(rr.getResult().getUserid(), image.getType(), image);
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
                    updateUserInfo(user, true);
                }
            }
            if (rr.isSuccess()) {
                this.sessions.set(sessionExpireSeconds, bean.getSessionid(), rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "wxlogin failed (" + bean + ")", e);
            return RetCodes.create(RET_USER_LOGIN_FAIL);
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
            int userid = Integer.parseInt(cookie.substring(sharp + 1, pos), 32);
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
        if (bean == null || bean.emptySessionid() || (user == null && bean.emptyAccount())) return RetCodes.create(RET_USER_ACCOUNT_PWD_ILLEGAL);
        String key = "";
        if (user == null && !bean.emptyAccount()) {
            if (bean.getAccount().indexOf('@') > 0) {
                key = "email";
                user = this.emailUserInfos.get(bean.getAccount());
            } else if (Character.isDigit(bean.getAccount().charAt(0))) {
                key = "mobile";
                user = this.mobileUserInfos.get(bean.getAccount());
            } else {
                key = "account";
                user = this.accountUserInfos.get(bean.getAccount());
            }
        }
        if (user == null) { //不在缓存内
            UserDetail detail = source.find(UserDetail.class, key, bean.getAccount());
            if (detail == null) return RetCodes.create(RET_USER_ACCOUNT_PWD_ILLEGAL);
            if (!detail.getPassword().equals(digestPassword(bean.getPassword()))) {
                return RetCodes.create(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
            }
            user = detail.createUserInfo();
            if (user.isFrobid()) return RetCodes.create(RET_USER_FREEZED);

            result.setRetcode(0);
            result.setResult(user);
            updateUserInfo(user, !user.getApptoken().equals(bean.getApptoken()));
        } else { //在缓存内
            if (unok && !user.getPassword().equals(digestPassword(bean.getPassword()))) {
                return RetCodes.create(RET_USER_ACCOUNT_PWD_ILLEGAL); //用户或密码错误   
            }
            result.setRetcode(0);
            result.setResult(user);
            if (!user.getApptoken().equals(bean.getApptoken())) { //用户设备变更了
                user.setApptoken(bean.getApptoken());
                source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", bean.getApptoken());
                updateUserInfo(user, true);
            }
        }
        this.sessions.set(sessionExpireSeconds, bean.getSessionid(), result.getResult().getUserid());
        return result;
    }

    //注销登录
    public boolean logout(final String sessionid) {
        UserInfo user = current(sessionid);
        if (user != null && user.isAp()) {
            user.setApptoken("");
            updateUserInfo(user, true);
        }
        sessions.remove(sessionid);
        return true;
    }

    public RetResult<RandomCode> checkRandomCode(String targetid, String randomcode) {
        if (randomcode == null || randomcode.isEmpty()) return RetCodes.create(RET_USER_RANDCODE_ILLEGAL);
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) randomcode = targetid + "-" + randomcode;
        RandomCode code = source.find(RandomCode.class, randomcode);
        return code == null ? RetCodes.create(RET_USER_RANDCODE_ILLEGAL) : (code.isExpired() ? RetCodes.create(RET_USER_RANDCODE_EXPIRED) : new RetResult(code));
    }

    public void removeRandomCode(RandomCode code) {
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
    }

    public RetResult updateUsername(int userid, String username) {
        if (username == null || username.isEmpty()) return RetCodes.create(RET_USER_USERNAME_ILLEGAL);
        UserInfo user = findUserInfo(userid);
        if (user.getUsername().equals(username)) return new RetResult();
        if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);
        user = user.copy();
        if (username.isEmpty()) return RetCodes.create(RET_USER_USERNAME_ILLEGAL);
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "username", username);
        source.updateColumn(UserDetail.class, user.getUserid(), "infotime", t);
        user.setUsername(username);
        user.setInfotime(t);
        updateUserInfo(user, true);
        return new RetResult();
    }

    public RetResult updateInfotime(int userid) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);
        user = user.copy();
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "infotime", t);
        user.setInfotime(t);
        updateUserInfo(user, false);
        return RetResult.SUCCESS;
    }

    public RetResult updateGender(int userid, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) return RetCodes.create(RET_USER_GENDER_ILLEGAL);
        user = user.copy();
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "gender", gender);
        user.setGender(gender);
        updateUserInfo(user, false);
        return RetResult.SUCCESS;
    }

    public RetResult updateMobile(int userid, String newmobile, String vercode) {
        int retcode = checkMobile(newmobile);
        if (retcode != 0) return RetCodes.create(retcode);
        RandomCode code = source.find(RandomCode.class, newmobile + "-" + vercode);
        if (code == null) return RetCodes.create(RET_USER_RANDCODE_ILLEGAL);
        if (code.isExpired()) return RetCodes.create(RET_USER_RANDCODE_EXPIRED);

        UserInfo user = findUserInfo(userid);
        if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);
        user = user.copy();
        source.updateColumn(UserDetail.class, user.getUserid(), "mobile", newmobile);
        user.setMobile(newmobile);
        updateUserInfo(user, true);
        code.setUserid(user.getUserid());
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
        return new RetResult();
    }

    public RetResult<UserInfo> updatePwd(UserPwdBean bean) {
        RetResult<UserInfo> result = new RetResult();
        UserInfo user = bean.getSessionid() == null ? null : current(bean.getSessionid());
        final String newpwd = digestPassword(secondPasswordMD5(bean.getNewpwd())); //HEX-MD5(密码明文)
        if (user == null) {  //表示忘记密码后进行重置密码
            bean.setSessionid(null);
            if (bean.getRandomcode() != null && !bean.getRandomcode().isEmpty()) {
                RandomCode code = source.find(RandomCode.class, bean.getRandomcode());
                if (code == null) return RetCodes.create(RET_USER_RANDCODE_ILLEGAL);
                if (code.isExpired()) return RetCodes.create(RET_USER_RANDCODE_EXPIRED);

                user = findUserInfo(code.getUserid());
                if (user == null) return RetCodes.create(RET_USER_NOTEXISTS);

                source.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
                user.setPassword(newpwd);
                source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
                source.delete(RandomCode.class, code.getRandomcode());
                updateUserInfo(user, false);
                result.setResult(user);
                return result;
            }
            result.setRetcode(1010002);
            return result;
        }
        //用户或密码错误
        if (!Objects.equals(user.getPassword(), digestPassword(secondPasswordMD5(bean.getOldpwd())))) {
            result.setRetcode(1010020); //原密码错误
            return result;
        }
        source.updateColumn(UserDetail.class, user.getUserid(), "password", newpwd);
        user.setPassword(newpwd);
        updateUserInfo(user, false);
        result.setResult(user);
        return result;
    }

    protected UserDetail findUserDetail(int userid) {
        return source.find(UserDetail.class, userid);
    }

    /**
     * 发送短信验证码
     *
     * @param type
     * @param mobile
     * @return
     */
    public RetResult smscode(final short type, String mobile) {
        if (mobile == null) return new RetResult(1010022, type + " mobile is null"); //手机号码无效
        if (mobile.indexOf('+') == 0) mobile = mobile.substring(1);
        UserInfo info = this.mobileUserInfos.get(mobile);
        if (type == RandomCode.TYPE_SMSREG || type == RandomCode.TYPE_SMSMOB) { //手机注册或手机修改的号码不能已存在
            if (info != null) return new RetResult(1010016, "smsreg or smsmob mobile " + mobile + " exists");
        } else if (type == RandomCode.TYPE_SMSPWD) { //修改密码
            if (info == null) return new RetResult(1010005, "smspwd mobile " + mobile + " not exists");
        } else {
            return new RetResult(1010004, type + " is illegal");
        }
        List<RandomCode> codes = source.queryList(RandomCode.class, FilterNode.create("randomcode", FilterExpress.LIKE, mobile + "-%"));
        if (!codes.isEmpty()) {
            RandomCode last = codes.get(codes.size() - 1);
            if (last.getCreatetime() + 60 * 1000 > System.currentTimeMillis()) return RetCodes.create(RET_USER_MOBILE_SMSFREQUENT);
        }
        final String sms = String.valueOf(RandomCode.randomSmsCode());
//        ResourceBundle bundle = ResourceBundle.getBundle(userbundle, Locale.forLanguageTag("zh"));
//        SmsMessage message = new SmsMessage();
//        message.setTo(mobile);
//        message.setContent(bundle.getString("vercode.mobile.content").replace("{randomcode}", sms));
//        try {
//            smsService.sendMessage(message);
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "mobile(" + mobile + ", type=" + type + ", locale = " + locale + ") send smscode error", e);
//            return new RetResult(1010025); //发送验证码失败
//        }
        RandomCode code = new RandomCode();
        code.setCreatetime(System.currentTimeMillis());
        if (info != null) code.setUserid(info.getUserid());
        code.setRandomcode(mobile + "-" + sms);
        code.setType(type);
        source.insert(code);
        return RetResult.SUCCESS;
    }

    /**
     *
     * 用户注册
     *
     * @param user
     * @return
     */
    public RetResult<UserInfo> register(UserDetail user) {
        RetResult<UserInfo> result = new RetResult();
        if (user == null) return RetCodes.create(RET_USER_SIGNUP_ILLEGAL);
        if (!user.isAc() && !user.isMb() && !user.isEm() && !user.isWx() && !user.isQq()) return RetCodes.create(RET_USER_SIGNUP_ILLEGAL);
        if (user.getGender() != 0 && user.getGender() != GENDER_MALE && user.getGender() != GENDER_FEMALE) return RetCodes.create(RET_USER_GENDER_ILLEGAL);
        int retcode = 0;
        if (user.isAc() && (retcode = checkAccount(user.getAccount())) != 0) return RetCodes.create(retcode);
        if (user.isAc() && (retcode = checkMobile(user.getMobile())) != 0) return RetCodes.create(retcode);
        if (user.isAc() && (retcode = checkEmail(user.getEmail())) != 0) return RetCodes.create(retcode);
        if (user.isWx() && wxunionidUserInfos.containsKey(user.getWxunionid())) return RetCodes.create(RET_USER_WXID_EXISTS);
        if (user.isQq() && qqopenidUserInfos.containsKey(user.getQqopenid())) return RetCodes.create(RET_USER_QQID_EXISTS);
        if (user.isMb()) {
            user.setRegtype(REGTYPE_MOBILE);
            if (user.getPassword().isEmpty()) return RetCodes.create(RET_USER_PASSWORD_ILLEGAL);
        } else if (user.isEm()) {
            user.setRegtype(REGTYPE_EMAIL);
            if (user.getPassword().isEmpty()) return RetCodes.create(RET_USER_PASSWORD_ILLEGAL);
        } else if (user.isWx()) {
            user.setRegtype(REGTYPE_WEIXIN);
        } else if (user.isQq()) {
            user.setRegtype(REGTYPE_QQOPEN);
        } else {
            user.setRegtype(REGTYPE_ACCOUNT);
            if (user.getPassword().isEmpty()) return RetCodes.create(RET_USER_PASSWORD_ILLEGAL);
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
        updateUserInfo(info, false);
        result.setResult(info);
        //可以在此处给企业微信号推送注册消息
        return result;
    }

    private static final Predicate<String> accountReg = Pattern.compile("^[a-zA-Z][\\w_.]{6,64}$").asPredicate();

    /**
     * 检测账号是否有效, 返回0表示手机号码可用
     * 账号不能以数字开头、不能包含@ ， 用于区分手机号码和邮箱
     *
     * @param account
     * @return
     */
    public int checkAccount(String account) {
        if (account == null) return RET_USER_ACCOUNT_ILLEGAL;
        if (!accountReg.test(account)) return RET_USER_ACCOUNT_ILLEGAL;
        return this.accountUserInfos.get(account) == null ? 0 : RET_USER_ACCOUNT_EXISTS;
    }

    private static final Predicate<String> mobileReg = Pattern.compile("^\\d{6,18}$").asPredicate();

    /**
     * 检测手机号码是否有效, 返回0表示手机号码可用
     *
     * @param mobile
     * @return
     */
    public int checkMobile(String mobile) {
        if (mobile == null) return RET_USER_MOBILE_ILLEGAL;
        if (!mobileReg.test(mobile)) return RET_USER_MOBILE_ILLEGAL;
        return this.mobileUserInfos.get(mobile) == null ? 0 : RET_USER_MOBILE_EXISTS;
    }

    private static final Predicate<String> emailReg = Pattern.compile("^(\\w|\\.|-)+@(\\w|-)+(\\.(\\w|-)+)+$").asPredicate();

    /**
     * 检测邮箱地址是否有效, 返回0表示邮箱地址可用.给新用户注册使用
     *
     * @param email
     * @return
     */
    public int checkEmail(String email) {
        if (email == null) return RET_USER_EMAIL_ILLEGAL;
        if (!emailReg.test(email)) return RET_USER_EMAIL_ILLEGAL;
        return this.emailUserInfos.get(email.toLowerCase()) == null ? 0 : RET_USER_EMAIL_EXISTS;
    }

    public boolean checkWxunionid(String wxunionid) {
        return wxunionid != null && !wxunionid.isEmpty() && this.wxunionidUserInfos.get(wxunionid) == null;
    }

    public boolean checkQqopenid(String qqopenid) {
        return qqopenid != null && !qqopenid.isEmpty() && this.qqopenidUserInfos.get(qqopenid) == null;
    }

    protected void updateUser(UserDetail user) {
        user.setUpdatetime(System.currentTimeMillis());
        source.update(user);
    }

    //AES加密
    public static String encryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            synchronized (aesEncrypter) {
                return Base64.getEncoder().encodeToString(aesEncrypter.doFinal(value.getBytes()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //AES解密
    public static String decryptAES(String value) {
        if (value == null || value.isEmpty()) return value;
        byte[] hex = Base64.getDecoder().decode(value);
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
