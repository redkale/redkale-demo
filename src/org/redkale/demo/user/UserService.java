/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import org.redkale.plugins.weixin.WeiXinMPService;
import org.redkale.plugins.email.EmailService;
import org.redkale.plugins.email.EmailMessage;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.annotation.*;
import javax.imageio.*;
import org.redkale.convert.json.*;
import org.redkale.demo.base.*;
import org.redkale.service.*;
import org.redkale.source.*;
import org.redkale.util.*;

import org.redkale.demo.file.*;
import static org.redkale.demo.user.UserDetail.*;

/**
 *
 * @author zhangjx
 */
public class UserService extends BaseService {

    public static final int RETCODE_NOUSER = 20100001; //用户不存在

    public static final int RETCODE_NOPERMISS = 20100002; //用户权限不对   

    public static final int RETCODE_TALKSMALL = 20100011; //语音太短  

    public static final int RETCODE_ILLPARAM = 20100021; //参数错误  

    protected final int sessionExpireSecond = 900; //15分钟

    protected final Map<String, UserInfo> wxunionidUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> qqopenidUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> emailUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> mobileUserInfos = new ConcurrentHashMap<>();

    protected final Map<String, UserInfo> apptokenUserInfos = new ConcurrentHashMap<>();

    protected final Map<Integer, UserInfo> userInfos = new ConcurrentHashMap<>();

    protected final AtomicInteger maxid = new AtomicInteger(200000000);

    @Resource(name = "reduser")
    private DataSource source;

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
                    while (flag.get()) {
                        synchronized (flipper) {
                            flipper.next();
                        }
                        Sheet<UserDetail> sheet = source.querySheet(UserDetail.class, flipper, (FilterBean) null);
                        if (sheet.isEmpty()) {
                            flag.set(false);
                            break;
                        }
                        if (sheet.getRows().size() < flipper.getSize()) flag.set(false);
                        for (UserDetail detail : sheet.getRows()) {
                            UserInfo info = detail.createUserInfo();
                            putUserInfo(info, false);
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

    public UserInfo findUserInfoByEmail(String email) {
        return this.emailUserInfos.get(email);
    }

    /**
     * 根据手机号码查找用户
     * <p>
     * @param mobile
     * @return
     */
    public UserInfo findUserInfoByMobile(String mobile) {
        return this.mobileUserInfos.get(mobile);
    }

    public UserInfo findUserInfoByWxunionid(String wxunionid) {
        if (wxunionid == null) return null;
        return this.wxunionidUserInfos.get(wxunionid);
    }

    public UserInfo findUserInfoByQqopenid(String qqopenid) {
        if (qqopenid == null) return null;
        return this.qqopenidUserInfos.get(qqopenid);
    }

    public UserInfo findUserInfoByApptoken(String apptoken) {
        if (apptoken == null) return null;
        return this.apptokenUserInfos.get(apptoken);
    }

    public UserInfo findUserInfo(int userid) {
        if (userid == UserInfo.USERID_SYSTEM) return UserInfo.USER_SYSTEM;
        UserInfo info = userInfos.get(userid);
        if (info != null) return info;
        UserDetail detail = source.find(UserDetail.class, userid);
        info = detail == null ? null : detail.createUserInfo();
        if (info != null) updateUserInfo(info, false);
        return info;
    }

    public Sheet<UserDetail> queryUserDetail(FilterNode node, Flipper flipper) {
        return source.querySheet(UserDetail.class, flipper, node);
    }

    public List<UserInfo> queryUserInfo(Collection<Integer> userids) {
        if (userids == null) return new ArrayList<>();
        List<UserInfo> list = new ArrayList<>(userids.size());
        for (int userid : userids) {
            if (userid == UserInfo.USERID_SYSTEM) {
                list.add(UserInfo.USER_SYSTEM);
            } else {
                UserInfo user = userInfos.get(userid);
                if (user != null) list.add(user);
            }
        }
        return list;
    }

    @MultiRun
    public void updateUserInfo(UserInfo info, boolean replace) {
        if (info.getUserid() > maxid.get()) maxid.set(info.getUserid());
        putUserInfo(info, replace);
        if (finer) logger.finer("updateUserInfo userinfo (" + info + ")");
    }

    private void putUserInfo(UserInfo info, boolean replace) {
        if (info == null) return;
        UserInfo old = userInfos.get(info.getUserid());
        if (replace) {
            if (old != null) {
                if (old.isEm()) emailUserInfos.remove(old.getEmail().toLowerCase());
                if (old.isMb()) mobileUserInfos.remove(old.getMobile());
                if (old.isWx()) wxunionidUserInfos.remove(old.getWxunionid());
                if (old.isQq()) qqopenidUserInfos.remove(old.getQqopenid());
                if (old.isHasapptoken()) apptokenUserInfos.remove(old.getApptoken());
            }
        }
        if (old != null) info = info.copyTo(old);
        userInfos.put(info.getUserid(), info);
        if (info.isEm()) emailUserInfos.put(info.getEmail().toLowerCase(), info);
        if (info.isMb()) mobileUserInfos.put(info.getMobile(), info);
        if (info.isWx()) wxunionidUserInfos.put(info.getWxunionid(), info);
        if (info.isQq()) qqopenidUserInfos.put(info.getQqopenid(), info);
        if (info.isHasapptoken()) apptokenUserInfos.put(info.getApptoken(), info);
    }

    public UserInfo current(String sessionid) {
        Integer userid = sessions.getAndRefresh(sessionid);
        return userid == null ? null : userInfos.get(userid);
    }

    public RetResult updateWxunionid(UserInfo user, String appid, String code) {
        try {
            Map<String, String> wxmap = wxMPService.getMPUserTokenByCode(appid, code);
            final String wxunionid = wxmap.get("unionid");
            if (wxunionid == null || wxunionid.isEmpty()) return new RetResult(1010011);
            if (!checkWxunionid(wxunionid)) return new RetResult(1010026);
            if (user == null) return new RetResult(1010005);
            user = user.copy();
            source.updateColumn(UserDetail.class, user.getUserid(), "wxunionid", wxunionid);
            user.setWxunionid(wxunionid);
            updateUserInfo(user, true);
            return new RetResult();
        } catch (Exception e) {
            logger.log(Level.FINE, "updateWxunionid failed (" + user + ", " + appid + ", " + code + ")", e);
            return new RetResult<>(1010011);
        }
    }

    private static String formatUserName(String name, String defname) {
        //A-Za-z0-9\\u4E00-\\uFFEE-\\. 
        StringBuilder sb = new StringBuilder(name.length());
        char first = 0;
        for (char ch : name.trim().toCharArray()) {
            if (first == 0) {
                first = ch;
                if (first >= '0' && first <= '9') sb.append("wx-");
            }
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch >= '\u4E00' && ch <= '\uFFEE') || ch == '_' || ch == '-' || ch == ' ' || ch == '.') {
                sb.append(ch);
            }
        }
        String rs = sb.toString().trim();
        return rs.length() < 2 ? (defname + rs) : rs;
    }

    public RetResult<UserInfo> qqlogin(QQLoginBean bean) {
        try {
            String qqappid = "xxxx";
            String url = "https://graph.qq.com/user/get_user_info?oauth_consumer_key=" + qqappid + "&access_token=" + bean.getAccesstoken() + "&openid=" + bean.getOpenid() + "&format=json";
            String json = Utility.getHttpContent(url);
            if (finest) logger.finest(url + "--->" + json);
            Map<String, String> jsonmap = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, json);
            if (!"0".equals(jsonmap.get("ret"))) return new RetResult(1010011, "qq get_user_info error.");
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByQqopenid(bean.getOpenid());
            if (user == null) {
                UserDetail detail = new UserDetail();
                detail.setUsername(formatUserName(jsonmap.getOrDefault("nickname", "qq-user"), "qq-"));
                detail.setQqopenid(bean.getOpenid());
                detail.setRegagent(bean.getReghost());
                detail.setRegaddr(bean.getRegaddr());
                String genstr = jsonmap.getOrDefault("gender", "");
                detail.setGender("男".equals(genstr) ? UserInfo.GENDER_MALE : ("女".equals(genstr) ? UserInfo.GENDER_FEMALE : (short) 0));
                logger.fine(bean + " --qqlogin-->" + convert.convertTo(jsonmap));
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
                this.sessions.set(sessionExpireSecond, bean.getSessionid(), rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "qqlogin failed (" + bean + ")", e);
            return new RetResult<>(1010011);
        }
    }

    /**
     * 微信登陆
     * <p>
     * @param bean
     * @return
     */
    public RetResult<UserInfo> wxlogin(WxLoginBean bean) {
        try {
            Map<String, String> wxmap = bean.emptyAccesstoken()
                    ? wxMPService.getMPUserTokenByCode(bean.getAppid(), bean.getCode())
                    : wxMPService.getMPUserTokenByOpenid(bean.getAccesstoken(), bean.getOpenid());
            final String unionid = wxmap.get("unionid");
            if (unionid == null) return new RetResult(1010011, "unionid is empty.");
            RetResult<UserInfo> rr;
            UserInfo user = findUserInfoByWxunionid(unionid);
            if (user == null) {
                if (!bean.isAutoreg()) return new RetResult(0, convert.convertTo(wxmap));
                UserDetail detail = new UserDetail();
                detail.setUsername(formatUserName(wxmap.getOrDefault("nickname", "wx-user"), "wx-"));
                detail.setWxunionid(unionid);
                detail.setApptoken(bean.getApptoken());
                detail.setRegagent(bean.getReghost());
                detail.setRegaddr(bean.getRegaddr());
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
                this.sessions.set(sessionExpireSecond, bean.getSessionid(), rr.getResult().getUserid());
            }
            return rr;
        } catch (Exception e) {
            logger.log(Level.FINE, "wxlogin failed (" + bean + ")", e);
            return new RetResult<>(1010011);
        }
    }

    public RetResult<UserInfo> login(LoginBean bean) {
        final RetResult<UserInfo> result = new RetResult();
        UserInfo user = null;
        boolean unok = true;
        if (bean != null && (bean.getCookieinfo() != null && bean.getCookieinfo().indexOf('$') > 0) && bean.emptyAccount()) {
            String cookie = bean.getCookieinfo();
            int sharp = cookie.indexOf('#');
            if (sharp > 0) bean.setApptoken(cookie.substring(0, sharp));
            int pos = cookie.indexOf('$');
            int userid = Integer.parseInt(cookie.substring(sharp + 1, pos), 32);
            user = this.findUserInfo(userid);
            if (user != null) {
                char type = cookie.charAt(pos + 1);
                char[] chars = cookie.substring(pos + 2).toCharArray();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < chars.length; i += 2) {
                    sb.append(chars[i]);
                }
                if (type == '0') {
                    bean.setPassword(sb.toString());
                } else if (type == '1') { //微信
                    if (!user.getWxunionid().isEmpty()) {
                        unok = !Objects.equals(sb.toString(), (user.getWxunionid()));
                    }
                } else if (type == '2') { //QQ
                    if (!user.getQqopenid().isEmpty()) {
                        unok = !Objects.equals(sb.toString(), (user.getQqopenid()));
                    }
                }
            }
        }
        if (bean == null || bean.emptySessionid() || (user == null && bean.emptyAccount())) {
            result.setRetcode(1010002); //用户或密码错误
            result.setRetinfo("login no sessionid or no account");
            return result;
        }
        boolean emailkind = (bean.getAccount() != null && bean.getAccount().indexOf('@') > 0);
        if (user == null) user = (emailkind ? this.emailUserInfos : this.mobileUserInfos).get(bean.getAccount());
        if (user == null) {
            UserDetail detail = source.find(UserDetail.class, emailkind ? "email" : "mobile", bean.getAccount());
            if (detail == null || !detail.checkPassword(bean.getPassword())) {
                result.setRetcode(1010002); //用户或密码错误                
                result.setRetinfo("login " + (emailkind ? "email" : "mobile") + "(" + bean.getAccount() + ") or password incorrect");
                //super.log(user, optionid, "用户账号或密码错误，登录失败.");
                return result;
            }
            result.setRetcode(0);
            user = detail.createUserInfo();

            if (user.isFrobid()) {
                result.setRetcode(1010003);
                //super.log(user, optionid, "用户被禁用，登录失败.");
                return result;
            }
            result.setResult(user);
            updateUserInfo(user, !user.getApptoken().equals(bean.getApptoken()));
        } else {
            if (unok && !Objects.equals(user.getPassword(), bean.getPassword())) {
                result.setRetcode(1010002); //用户或密码错误
                result.setRetinfo("login password incorrect");
                //super.log(user, optionid, "用户账号或密码错误，登录失败.");
                return result;
            }
            result.setRetcode(0);
            result.setResult(user);
            if (!user.getApptoken().equals(bean.getApptoken())) {
                user.setApptoken(bean.getApptoken());
                source.updateColumn(UserDetail.class, user.getUserid(), "apptoken", bean.getApptoken());
                updateUserInfo(user, true);
            }
        }
        this.sessions.set(sessionExpireSecond, bean.getSessionid(), result.getResult().getUserid());
        return result;
    }

    public boolean logout(final String sessionid) {
        UserInfo user = current(sessionid);
        if (user != null && user.isHasapptoken()) {
            user.setApptoken("");
            updateUserInfo(user, true);
        }
        sessions.remove(sessionid);
        return true;
    }

    public RetResult<RandomCode> checkRandomCode(String targetid, String randomcode) {
        if (randomcode == null || randomcode.isEmpty()) return new RetResult(1010021);
        if (targetid != null && targetid.length() > 5 && randomcode.length() < 30) randomcode = targetid + "-" + randomcode;
        RandomCode code = source.find(RandomCode.class, randomcode);
        return code == null ? new RetResult(1010021) : (code.isExpired() ? new RetResult(1010021) : new RetResult(code));
    }

    public void removeRandomCode(RandomCode code) {
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
    }

    public RetResult updateUsername(int userid, String username) {
        if (username == null || (!username.isEmpty() && !checkUsername(username))) return new RetResult(1010013);
        UserInfo user = findUserInfo(userid);
        if (user.getUsername().equals(username)) return new RetResult();
        if (user == null) return new RetResult(1010005);
        user = user.copy();
        if (username.isEmpty()) return new RetResult(1010013);
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "username", username);
        source.updateColumn(UserDetail.class, user.getUserid(), "infotime", t);
        user.setUsername(username);
        user.setInfotime(t);
        updateUserInfo(user, true);
        return new RetResult();
    }

    /**
     * 更新新邮箱， 只对后门接口有效
     * <p>
     * @param userid
     * @param newmail
     * @return
     */
    public RetResult updateEmail(int userid, String newmail) {
        if (newmail != null && !newmail.isEmpty() && !checkEmail(newmail)) return new RetResult(1010011);
        UserInfo user = findUserInfo(userid);
        if (user == null) return new RetResult(1010005);
        if ((newmail == null || newmail.isEmpty()) && !user.isMb() && !user.isQq() && !user.isWx()) return new RetResult(1010011); //当手机为空时邮箱不能为空
        user = user.copy();
        source.updateColumn(UserDetail.class, user.getUserid(), "email", newmail);
        user.setEmail(newmail);
        updateUserInfo(user, true);
        return new RetResult();
    }

    public RetResult updateInfotime(int userid) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return new RetResult(1010005);
        user = user.copy();
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "infotime", t);
        user.setInfotime(t);
        updateUserInfo(user, false);
        return RetResult.SUCCESS;
    }

    public RetResult updateGender(int userid, short gender) {
        UserInfo user = findUserInfo(userid);
        if (user == null) return new RetResult(1010005);
        if (gender != GENDER_MALE && gender != GENDER_FEMALE) return new RetResult(1010005);
        user = user.copy();
        long t = System.currentTimeMillis();
        source.updateColumn(UserDetail.class, user.getUserid(), "gender", gender);
        user.setGender(gender);
        updateUserInfo(user, false);
        return RetResult.SUCCESS;
    }

    public RetResult updateMobile(int userid, String newmobile, String vercode) {
        if (!checkMobile(newmobile)) return new RetResult(1010016);
        RandomCode code = source.find(RandomCode.class, newmobile + "-" + vercode);
        if (code == null || code.isExpired()) return new RetResult(1010021); //验证码无效
        UserInfo user = findUserInfo(userid);
        if (user == null) return new RetResult(1010005);
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
        if (user == null) {
            bean.setSessionid(null);
            if (bean.getRandomcode() != null && !bean.getRandomcode().isEmpty()) {
                RandomCode code = source.find(RandomCode.class, bean.getRandomcode());
                if (code == null || code.isExpired()) {//验证码无效
                    result.setRetcode(1010021);
                    return result;
                }
                user = findUserInfo(code.getUserid());
                if (user == null) {//验证码无效
                    result.setRetcode(1010021);
                    return result;
                }
                source.updateColumn(UserDetail.class, user.getUserid(), "password", bean.getNewpwd());
                user.setPassword(bean.getNewpwd());
                source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
                source.delete(RandomCode.class, code.getRandomcode());
                updateUserInfo(user, false);
                result.setResult(user);
                return result;
            }
            result.setRetcode(1010002);
            return result;
        } //用户或密码错误
        if (!Objects.equals(user.getPassword(), bean.getOldpwd())) {
            result.setRetcode(1010020); //原密码错误
            return result;
        }
        source.updateColumn(UserDetail.class, user.getUserid(), "password", bean.getNewpwd());
        user.setPassword(bean.getNewpwd());
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
        if (type == RandomCode.TYPE_SMSREG || type == RandomCode.TYPE_SMSMOB) {
            if (info != null) return new RetResult(1010016, "smsreg or smsmob mobile " + mobile + " exists");
        } else if (type == RandomCode.TYPE_SMSPWD) {
            if (info == null) return new RetResult(1010005, "smspwd mobile " + mobile + " not exists");
        } else {
            return new RetResult(1010004, type + " is illegal");
        }
        List<RandomCode> codes = source.queryList(RandomCode.class, FilterNode.create("randomcode", FilterExpress.LIKE, mobile + "-%"));
        if (!codes.isEmpty()) {
            RandomCode last = codes.get(codes.size() - 1);
            if (last.getCreatetime() + 60 * 1000 > System.currentTimeMillis()) return new RetResult(1010024, "mobile " + mobile + " send smscode frequently");
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
     * 忘记密码, 0表示发送邮箱认证成功
     *
     * @param email
     * @param wbhost
     * @return
     */
    public int forgetpwd(String email, String wbhost) {
        long t1 = System.currentTimeMillis();
        if (email == null) return 1010019; //邮箱地址无效
        UserInfo info = this.emailUserInfos.get(email.toLowerCase());
        if (info == null) return 1010019; //邮箱地址无效
        RandomCode code = new RandomCode();
        code.setCreatetime(System.currentTimeMillis());
        code.setUserid(info.getUserid());
        code.setRandomcode(RandomCode.randomLongCode());
        code.setType(RandomCode.TYPE_MAILPWD);
        source.insert(code);
        ResourceBundle bundle = ResourceBundle.getBundle(userbundle, Locale.forLanguageTag("zh"));
        EmailMessage message = new EmailMessage();
        message.setTo(email);
        message.setTitle(bundle.getString("setpwd.email.title"));
        String content = bundle.getString("setpwd.email.html")
                .replace("{username}", info.getUsername())
                .replace("{useremail}", info.getEmail())
                .replace("{randomcode}", code.getRandomcode());
        message.setContent(content);
        try {
            emailService.sendMessage(message);
        } catch (Exception e) {
            return 1010017; //发送激活邮件失败
        }
        return 0;
    }

    /**
     *
     * 1010001	未登陆
     * 1010002	用户或密码错误
     * 1010003	用户状态异常
     * 1010004	验证码错误或失效
     * 1010005	用户不存在
     * 1010010	邮箱或手机号码已存在
     * 1010011	注册参数异常
     * 1010012	注册类型错误
     * 1010013	用户姓名错误
     * 1010014	用户名无效或已存在
     * 1010015	注册邮箱地址无效或已存在
     * 1010016	注册手机号码无效或已存在
     * 1010017	发送激活邮件失败
     * 1010018	注册激活码无效
     * 1010019	邮箱地址无效
     * 1010020	原密码错误
     * 1010021	激活码无效
     * 1010022	手机号码无效
     * 1010023 用户已绑定邮箱
     *
     *
     * @param user
     * @return
     */
    public RetResult<UserInfo> register(UserDetail user) {
        RetResult<UserInfo> result = new RetResult();
        if (user == null) {  //   注册参数异常
            result.setRetinfo("parameter is empty");
            result.setRetcode(1010011);
            return result;
        }
        if (user.getEmail().isEmpty() && user.getMobile().isEmpty() && user.getWxunionid().isEmpty() && user.getQqopenid().isEmpty()) {
            result.setRetcode(1010011);
            result.setRetinfo("email, mobile, wxid, qqid is empty");
            return result;
        }
        if (!checkUsername(user.getUsername())) {//  用户名错误  
            result.setRetcode(1010014);
            return result;
        }
        if (!user.getEmail().isEmpty() && !checkEmail(user.getEmail())) {
            result.setRetcode(1010015);
            return result;
        }
        if (!user.getMobile().isEmpty() && !checkMobile(user.getMobile())) {
            result.setRetcode(1010016);
            return result;
        }
        if (!user.getWxunionid().isEmpty() && wxunionidUserInfos.containsKey(user.getWxunionid())) {
            result.setRetcode(1010026);
            return result;
        }
        if (!user.getQqopenid().isEmpty() && qqopenidUserInfos.containsKey(user.getQqopenid())) {
            result.setRetcode(1010027);
            return result;
        }
        if (!user.getWxunionid().isEmpty()) {
            user.setRegtype(REGTYPE_WEIXIN);
        } else if (!user.getQqopenid().isEmpty()) {
            user.setRegtype(REGTYPE_QQOPEN);
        } else if (!user.getMobile().isEmpty()) {
            user.setRegtype(REGTYPE_MOBILE);
        } else {
            user.setRegtype(REGTYPE_EMAIL);
        }
        user.setUserid(maxid.incrementAndGet());
        user.setCreatetime(System.currentTimeMillis());
        user.digestPassword(user.getPassword());
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

    private static final Predicate<String> emailReg = Pattern.compile("^(\\w|\\.|-)+@(\\w|-)+(\\.(\\w|-)+)+$").asPredicate();

    /**
     * 检测邮箱地址是否有效, 返回true表示邮箱地址可用.给新用户注册使用
     *
     * @param email
     * @return
     */
    public boolean checkEmail(String email) {
        if (email == null) return false;
        if (!emailReg.test(email)) return false;
        return this.emailUserInfos.get(email.toLowerCase()) == null;
    }

    public boolean checkWxunionid(String wxunionid) {
        return wxunionid != null && !wxunionid.isEmpty() && this.wxunionidUserInfos.get(wxunionid) == null;
    }

    public boolean checkQqopenid(String qqopenid) {
        return qqopenid != null && !qqopenid.isEmpty() && this.qqopenidUserInfos.get(qqopenid) == null;
    }

    private static final Predicate<String> mobileReg = Pattern.compile("^\\d{6,18}$").asPredicate();

    /**
     * 检测手机号码是否有效, 返回true表示手机号码可用
     *
     * @param mobile
     * @return
     */
    public boolean checkMobile(String mobile) {
        if (mobile == null) return false;
        if (!mobileReg.test(mobile)) return false;
        return this.mobileUserInfos.get(mobile) == null;
    }

    private static final Predicate<String> userNameReg = Pattern.compile("^[A-Za-z0-9\\u4E00-\\uFFEE-\\. _]{2,64}$").asPredicate();

    /**
     * 检测用户名是否有效, 返回true表示用户名可用
     *
     * @param username
     * @return
     */
    public boolean checkUsername(String username) {
        if (username == null) return false;
        return userNameReg.test(username);
    }

    protected void updateUser(UserDetail user) {
        user.setUpdatetime(System.currentTimeMillis());
        source.update(user);
    }

    /**
     * 修改用户邮件
     *
     * @param userInfo
     * @param email
     * @param vercode
     * @return
     */
    public RetResult updateEmail(UserInfo userInfo, String email, String vercode) {
        RetResult result = new RetResult();
        if (email != null && !email.isEmpty() && !checkEmail(email)) return new RetResult(1010011);
        RandomCode code = source.find(RandomCode.class, email + "-" + vercode);
        if (code == null || code.isExpired()) return new RetResult(1010021); //验证码无效
        userInfo = userInfo.copy();
        source.updateColumn(UserDetail.class, userInfo.getUserid(), "email", email);
        userInfo.setEmail(email);
        updateUserInfo(userInfo, true);
        source.insert(code.createRandomCodeHis(RandomCodeHis.RETCODE_OK));
        source.delete(RandomCode.class, code.getRandomcode());
        return result;
    }

}
