/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import org.redkale.annotation.Resource;
import org.redkale.convert.json.*;
import org.redkale.demo.base.*;
import org.redkale.demo.notice.RandomCode;
import org.redkale.demo.weixin.WeiXinMPService;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.AnyValue;

/**
 * 用户模块的Servlet
 *
 * @author zhangjx
 */
@WebServlet({"/user/*"})
public class UserServlet extends BaseServlet {

    public static final String COOKIE_AUTOLOGIN = "UNF";

    @Resource
    private UserService service;

    //用于微信登录
    @Resource
    private WeiXinMPService wxService;

    @Resource
    private JsonConvert userConvert;

    @Override
    public void init(HttpContext context, AnyValue config) {
        JsonFactory factory = JsonFactory.root().createChild();
        //当前用户查看自己的用户信息时允许输出隐私信息
        factory.register(UserDetail.class, false, "mobile", "email", "wxunionid", "qqopenid", "appToken");
        userConvert = factory.getConvert();
        super.init(context, config);
    }

    //用户注销
    @HttpMapping(url = "/user/logout", auth = false)
    public void logout(HttpRequest req, HttpResponse resp) throws IOException {
        String sessionid = req.getSessionid(false);
        if (sessionid != null) {
            service.logout(sessionid);
        }
        HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, "");
        cookie.setPath("/");
        cookie.setMaxAge(1);
        resp.addCookie(cookie);
        resp.finish("{\"success\":true}");
    }

    @HttpMapping(url = "/user/updatewxid", auth = false)
    public void updateWxunionid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) {
            logger.finest("/user/updatewxid :  " + code + "," + state);
        }
        service.updateWxunionid(service.findUserInfo(req.currentUserid(int.class)), code);
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    //需要在 “开发 - 接口权限 - 网页服务 - 网页帐号 - 网页授权获取用户基本信息”的配置选项中，修改授权回调域名
    @HttpMapping(url = "/user/wxopenid", auth = false)
    public void wxopenid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        if (finest) {
            logger.finest("/user/wxopenid :  " + req);
        }
        Map<String, String> rr = wxService.getMPUserTokenByCode(code).join();
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    /**
     * 微信登录 https://open.weixin.qq.com/connect/qrconnect?appid=wx微信ID&redirect_uri=xxxxx&response_type=code&scope=snsapi_login&state=wx微信ID_1#wechat_redirect
     * 接收两种形式：
     * WEB端微信登录： /user/wxlogin?code=XXXXXX&state=wx微信ID_1&appToken=XXX
     * APP端微信登录: /user/wxlogin?openid=XXXX&state=1&access_token=XXX&appToken=XXX
     * <p>
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/wxlogin", auth = false)
    public void wxlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag

        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");

        if (finest) {
            logger.finest("/user/wxlogin :  code = " + code + ", access_token = " + access_token + ", openid = " + openid + ", state =" + state);
        }
        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;
        if (appid.length() < 2) {
            appid = "";
        }
        boolean autoreg = (pos > 0 || "1".equals(state)) ? (state.charAt(pos + 1) == '1') : true;
        final boolean wxbrowser = req.getHeader("User-Agent", "").contains("MicroMessenger");
        LoginWXBean bean = new LoginWXBean();
        { //WEB方式
            bean.setAppid(appid);
            bean.setCode(code);
        }
        { //APP方式
            bean.setAccessToken(access_token);
            bean.setOpenid(openid);
        }
        bean.setAutoreg(autoreg);
        bean.setAppToken(req.getParameter("appToken", ""));
        bean.setLoginAddr(req.getRemoteAddr());
        bean.setLoginAgent(req.getHeader("User-Agent"));
        if (autoreg) {
            bean.setSessionid(req.changeSessionid());
        }
        RetResult<UserInfo> rr = service.wxlogin(bean);
        if (autoreg && rr.isSuccess() && (wxbrowser || (access_token != null && !access_token.isEmpty()))) {
            UserInfo info = rr.getResult();
            int age = 1000 * 24 * 60 * 60;
            String key = (bean.emptyAppToken() ? "" : (bean.getAppToken() + "#")) + info.getUser36id() + "$1" + info.getWxunionid() + "?" + age + "-" + System.currentTimeMillis();
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(age);
            resp.addCookie(cookie);
        }
        if (access_token == null || access_token.isEmpty()) { //WEB登录
            resp.setHeader("Location", req.getParameter("url", "/"));
            resp.finish(302, null);
        } else { //APP 模式
            resp.finishJson(rr);
        }
    }

    @HttpMapping(url = "/user/qqlogin", auth = false)
    public void qqlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        if (finest) {
            logger.finest("/user/qqlogin :  " + openid + "," + access_token);
        }
        LoginQQBean bean = new LoginQQBean();
        bean.setAccessToken(access_token);
        bean.setAppToken(req.getParameter("appToken", ""));
        bean.setOpenid(openid);
        bean.setLoginAddr(req.getRemoteAddr());
        bean.setLoginAgent(req.getHeader("User-Agent"));
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.qqlogin(bean);
        if (rr.isSuccess()) {
            UserInfo info = rr.getResult();
            int age = 1000 * 24 * 60 * 60;
            String key = info.getUser36id() + "$2" + info.getQqopenid() + "?" + age + "-" + System.currentTimeMillis();
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(age);
            resp.addCookie(cookie);
        }
        if (access_token == null || access_token.isEmpty()) {
            resp.setHeader("Location", req.getParameter("url", "/"));
            resp.finish(302, null);
        } else { //APP 模式
            resp.finishJson(rr);
        }
    }

    /**
     * 用户登录
     *
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/login", auth = false)
    public void login(HttpRequest req, HttpResponse resp) throws IOException {
        LoginBean bean = req.getJsonParameter(LoginBean.class, "bean");
        if (bean == null) {
            bean = new LoginBean();
        }
        if (!bean.emptyPassword()) {
            bean.setPassword(UserService.secondPasswordMD5(bean.getPassword()));
        }
        bean.setLoginAgent(req.getHeader("User-Agent"));
        bean.setLoginIp(req.getRemoteAddr());
        String oldsessionid = req.getSessionid(false);
        if (oldsessionid != null && !oldsessionid.isEmpty()) {
            service.logout(oldsessionid);
        }
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> result = service.login(bean);
        if (result.isSuccess() && !bean.emptyPassword()) { //必须是密码登录类
            if (bean.getCacheDay() > 0 && bean.emptyCookieInfo()) {  //保存N天 
                UserInfo info = result.getResult();
                int age = bean.getCacheDay() * 24 * 60 * 60;
                String key = (bean.emptyAppToken() ? "" : (bean.getAppToken() + "#")) + info.getUser36id() + "$0" + bean.getPassword() + "?" + age + "-" + System.currentTimeMillis();
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(age);
                resp.addCookie(cookie);
            }
        }
        resp.finishJson(result);
    }

    @HttpMapping(url = "/user/signup", auth = false)   //待定
    public void signup(HttpRequest req, HttpResponse resp) throws IOException {
        long s = System.currentTimeMillis();
        Map<String, String> map = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, req.getParameter("bean"));
        RetResult<RandomCode> ret = null;
        String beanaccount;
        UserDetail bean = new UserDetail();
        if (map.containsKey("mobile")) {
            bean.setMobile(map.get("mobile"));
            beanaccount = bean.getMobile();
            ret = service.checkRandomCode(bean.getMobile(), map.get("verCode"), RandomCode.TYPE_SMSREG);
            if (!ret.isSuccess()) {
                resp.finishJson(ret);
                return;
            }
        } else if (map.containsKey("email")) {
            bean.setEmail(map.get("email"));
            beanaccount = bean.getEmail();
        } else {
            bean.setAccount(map.getOrDefault("account", ""));
            beanaccount = bean.getAccount();
        }
        bean.setUserName(map.getOrDefault("userName", ""));
        bean.setAppToken(map.getOrDefault("appToken", ""));
        bean.setPassword(map.getOrDefault("password", ""));
        bean.setRegAddr(req.getRemoteAddr());
        bean.setRegAgent(req.getHeader("User-Agent", ""));
        final String reqpwd = bean.getPassword();
        RetResult<UserInfo> rr = service.register(bean);
        if (rr.isSuccess()) {
            if (ret != null) {
                ret.getResult().setUserid(rr.getResult().getUserid());
                service.removeRandomCode(ret.getResult());
            }
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(beanaccount);
            loginbean.setAppToken(bean.getAppToken());
            loginbean.setPassword(UserService.secondPasswordMD5(reqpwd));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginAgent(req.getHeader("User-Agent"));
            if (map.containsKey("cacheDay")) {
                loginbean.setCacheDay(Integer.parseInt(map.getOrDefault("cacheDay", "0")));
            }
            loginbean.setLoginIp(req.getRemoteAddr());
            rr = service.login(loginbean);
        }
        long e = System.currentTimeMillis() - s;
        if (e > 500) {
            logger.warning("/user/signup cost " + e / 1000.0 + " seconds " + bean);
        }
        resp.finishJson(rr);
    }

    /**
     * 修改密码
     *
     * @param req
     * @param resp
     *
     * @throws IOException
     */
    @HttpMapping(url = "/user/updatepwd")
    public void updatepwd(HttpRequest req, HttpResponse resp) throws IOException {
        UserPwdBean bean = req.getJsonParameter(UserPwdBean.class, "bean");
        UserInfo curr = service.findUserInfo(req.currentUserid(int.class));
        if (curr != null) {
            bean.setSessionid(req.getSessionid(false));
        }
        RetResult<UserInfo> result = service.updatePwd(bean);
        if (result.isSuccess() && curr == null) { //找回的密码
            curr = result.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.getEmail().isEmpty() ? curr.getMobile() : curr.getEmail());
            loginbean.setPassword(UserService.secondPasswordMD5(bean.getNewpwd()));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginAgent(req.getHeader("User-Agent"));
            loginbean.setLoginIp(req.getRemoteAddr());
            result = service.login(loginbean);
        }
        String autologin = req.getCookie(COOKIE_AUTOLOGIN);
        if (result.isSuccess() && autologin != null) {
            autologin = UserService.decryptAES(autologin);
            if (autologin.contains("$0")) { //表示COOKIE_AUTOLOGIN 为密码类型存储
                String newpwd = UserService.secondPasswordMD5(bean.getNewpwd());
                int wen = autologin.indexOf('?');
                int mei = autologin.indexOf('$');
                String key = autologin.substring(0, mei + 2) + newpwd + autologin.substring(wen);
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, UserService.encryptAES(key));
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                String time = autologin.substring(wen + 1);
                int fen = time.indexOf('-');
                int age = Integer.parseInt(time.substring(0, fen)); //秒数
                long point = Long.parseLong(time.substring(fen + 1)); //毫秒数
                cookie.setMaxAge(age - (System.currentTimeMillis() - point) / 1000);
                resp.addCookie(cookie);
            }
        }
        resp.finishJson(result);
    }

    //更新用户手机号码
    @HttpMapping(url = "/user/updatemobile")
    public void updatemobile(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finishJson(service.updateMobile(user.getUserid(), req.getParameter("mobile"), req.getParameter("verCode"), req.getParameter("precode")));
    }

    //更新用户昵称
    @HttpMapping(url = "/user/updateuserName")
    public void updateUserName(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finishJson(service.updateUserName(user.getUserid(), req.getParameter("userName")));
    }

    //更新设备ID
    @HttpMapping(url = "/user/updateappToken")
    public void updateAppToken(HttpRequest req, HttpResponse resp) throws IOException {
        String s = req.getPathLastParam();
        if ("updateappToken".equalsIgnoreCase(s)) {
            s = "";
        }
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finishJson(service.updateAppToken(user.getUserid(), req.getParameter("appos", req.getPathParam("appos:", "")), req.getParameter("appToken", s)));
    }

    //更新性别
    @HttpMapping(url = "/user/updategender/")
    public void updateGender(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finishJson(service.updateGender(user.getUserid(), Short.parseShort(req.getPathLastParam())));
    }

    //发送修改密码验证码
    @HttpMapping(url = "/user/smspwdcode")
    public void smscode(HttpRequest req, HttpResponse resp) throws IOException {
        smsverCode(RandomCode.TYPE_SMSPWD, req, resp);
    }

    //发送手机修改验证码
    @HttpMapping(url = "/user/smsmobcode")
    public void smsmob(HttpRequest req, HttpResponse resp) throws IOException {
        smsverCode(RandomCode.TYPE_SMSMOB, req, resp);
    }

    //发送原手机验证码
    @HttpMapping(url = "/user/smsodmcode")
    public void smsodm(HttpRequest req, HttpResponse resp) throws IOException {
        smsverCode(RandomCode.TYPE_SMSODM, req, resp);
    }

    //发送手机注册验证码
    @HttpMapping(url = "/user/smsregcode", auth = false)
    public void smsreg(HttpRequest req, HttpResponse resp) throws IOException {
        smsverCode(RandomCode.TYPE_SMSREG, req, resp);
    }

    //发送手机登录验证码
    @HttpMapping(url = "/user/smslgncode", auth = false)
    public void smslgn(HttpRequest req, HttpResponse resp) throws IOException {
        smsverCode(RandomCode.TYPE_SMSLGN, req, resp);
    }

    private void smsverCode(final short type, HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getPathParam("mobile:", req.getParameter("mobile"));
        if (type == RandomCode.TYPE_SMSODM) { //给原手机号码发送验证短信
            UserInfo user = service.findUserInfo(req.currentUserid(int.class));
            if (user != null) {
                mobile = user.getMobile();
            }
        }
        RetResult rr = service.smscode(type, mobile);
        if (finest) {
            logger.finest(req.getRequestPath() + ", mobile = " + mobile + "---->" + rr);
        }
        resp.finishJson(rr);
    }

    //检测账号是否有效, 返回t0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkaccount/", auth = false)
    public void checkAccount(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkAccount(req.getPathLastParam())));
    }

    //检测手机号码是否有效, 返回0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkmobile/", auth = false)
    public void checkMobile(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkMobile(req.getPathLastParam())));
    }

    //检测邮箱地址是否有效, 返回0表示可用.给新用户注册使用
    @HttpMapping(url = "/user/checkemail/", auth = false)
    public void checkEmail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(RetCodes.retResult(service.checkEmail(req.getPathLastParam())));
    }

    //验证短信验证码
    @HttpMapping(url = "/user/checkcode", auth = false)
    public void checkcode(HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getPathParam("mobile:", req.getParameter("mobile"));
        String verCode = req.getPathParam("verCode:", req.getParameter("verCode"));
        RetResult<RandomCode> ret = service.checkRandomCode(mobile, verCode, (short) 0);
        resp.finishJson(RetCodes.retResult(ret.getRetcode()));
    }

    //获取当前用户基本信息
    @HttpMapping(url = "/user/myinfo")
    public void myinfo(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finishJson(user);
    }

    //获取当前用户基本信息（js格式）
    @HttpMapping(url = "/user/js/myinfo", auth = false)
    public void myinfojs(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var userself = null;");
        } else {
            resp.finish("var userself = " + convert.convertTo(user) + ";");
        }
    }

    //获取个人基本信息
    @HttpMapping(url = "/user/info/")
    public void info(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.findUserInfo(Integer.parseInt(req.getPathLastParam(), 36)));
    }

    //获取当前用户详细信息
    @HttpMapping(url = "/user/mydetail")
    public void mydetail(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.finish(userConvert.convertTo(service.findUserDetail(user.getUserid())));
    }

    //获取当前用户详细信息（js格式）
    @HttpMapping(url = "/user/js/mydetail", auth = false)
    public void mydetailjs(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = service.findUserInfo(req.currentUserid(int.class));
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var userdetailself = null;");
        } else {
            resp.finish("var userdetailself = " + userConvert.convertTo(service.findUserDetail(user.getUserid())) + ";");
        }
    }

}
