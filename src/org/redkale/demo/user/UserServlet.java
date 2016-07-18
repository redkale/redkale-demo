/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import javax.annotation.Resource;
import org.redkale.convert.json.*;
import org.redkale.demo.base.*;
import org.redkale.net.http.*;
import org.redkalex.weixin.WeiXinMPService;
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
        factory.register(UserDetail.class, false, "mobile", "email", "wxunionid", "qqopenid", "apptoken");
        userConvert = factory.getConvert();
        super.init(context, config);
    }

    //用户注销
    @AuthIgnore
    @WebAction(url = "/user/logout")
    public void logout(HttpRequest req, HttpResponse resp) throws IOException {
        String sessionid = req.getSessionid(false);
        if (sessionid != null) service.logout(sessionid);
        HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, "");
        cookie.setPath("/");
        cookie.setMaxAge(1);
        resp.addCookie(cookie);
        resp.finish("{\"success\":true}");
    }

    @WebAction(url = "/user/updatewxid")
    public void updateWxunionid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) logger.finest("/user/updatewxid :  " + code + "," + state);
        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;
        service.updateWxunionid(currentUser(req), appid, code);
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    @WebAction(url = "/user/wxopenid")
    public void wxopenid(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) logger.finest("/user/wxopenid :  " + code + "," + state);
        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;

        Map<String, String> rr = wxService.getMPUserTokenByCode(appid, code);
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    /**
     * 微信登陆 https://open.weixin.qq.com/connect/qrconnect?appid=wx微信ID&redirect_uri=xxxxx&response_type=code&scope=snsapi_login&state=wx微信ID_1#wechat_redirect
     * 接收两种形式：
     *  WEB端微信登录： /user/wxlogin?code=XXXXXX&state=wx微信ID_1&apptoken=XXX
     *  APP端微信登录:  /user/wxlogin?openid=XXXX&state=1&access_token=XXX&apptoken=XXX
     * <p>
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/wxlogin")
    public void wxlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        
        if (finest) logger.finest("/user/wxlogin :  code = " + code + ", access_token = " + access_token + ", openid = " + openid + ", state =" + state);
        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;
        if (appid.length() < 2) appid = "";
        boolean autoreg = (pos > 0 || "1".equals(state)) ? (state.charAt(pos + 1) == '1') : true;
        final boolean wxbrowser = req.getHeader("User-Agent", "").contains("MicroMessenger");
        LoginWXBean bean = new LoginWXBean();
        { //WEB方式
            bean.setAppid(appid);
            bean.setCode(code);
        }
        { //APP方式
            bean.setAccesstoken(access_token);
            bean.setOpenid(openid);
        }
        bean.setAutoreg(autoreg);
        bean.setApptoken(req.getParameter("apptoken", ""));
        bean.setLoginaddr(req.getRemoteAddr());
        bean.setLoginagent(req.getHeader("User-Agent"));
        if (autoreg) bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.wxlogin(bean);
        if (autoreg && rr.isSuccess() && (wxbrowser || (access_token != null && !access_token.isEmpty()))) {
            UserInfo info = rr.getResult();
            int age = 1000 * 24 * 60 * 60;
            String key = UserService.encryptAES((bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + info.getUser36id() + "$1" + info.getWxunionid() + "?" + age + "-" + System.currentTimeMillis());
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
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

    @AuthIgnore
    @WebAction(url = "/user/qqlogin")
    public void qqlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        if (finest) logger.finest("/user/qqlogin :  " + openid + "," + access_token);
        LoginQQBean bean = new LoginQQBean();
        bean.setAccesstoken(access_token);
        bean.setApptoken(req.getParameter("apptoken", ""));
        bean.setOpenid(openid);
        bean.setLoginaddr(req.getRemoteAddr());
        bean.setLoginagent(req.getHeader("User-Agent"));
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.qqlogin(bean);
        if (rr.isSuccess()) {
            UserInfo info = rr.getResult();
            int age = 1000 * 24 * 60 * 60;
            String key = UserService.encryptAES(info.getUser36id() + "$2" + info.getQqopenid() + "?" + age + "-" + System.currentTimeMillis());
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
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
     * 用户登陆
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/login")
    public void login(HttpRequest req, HttpResponse resp) throws IOException {
        LoginBean bean = req.getJsonParameter(LoginBean.class, "bean");
        if (bean == null) bean = new LoginBean();
        if (!bean.emptyPassword()) bean.setPassword(UserService.secondPasswordMD5(bean.getPassword()));
        bean.setLoginagent(req.getHeader("User-Agent"));
        bean.setLoginip(req.getRemoteAddr());
        String oldsessionid = req.getSessionid(false);
        if (oldsessionid != null && !oldsessionid.isEmpty()) service.logout(oldsessionid);
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> result = service.login(bean);
        if (result.isSuccess()) {
            if (bean.getCacheday() > 0 && bean.emptyCookieinfo()) {  //保存N天 
                UserInfo info = result.getResult();
                int age = bean.getCacheday() * 24 * 60 * 60;
                String key = UserService.encryptAES((bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + info.getUser36id() + "$0" + bean.getPassword() + "?" + age + "-" + System.currentTimeMillis());
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(age);
                resp.addCookie(cookie);
            }
        }
        sendRetResult(resp, result);
    }

    @AuthIgnore
    @WebAction(url = "/user/signup")   //待定
    public void signup(HttpRequest req, HttpResponse resp) throws IOException {
        long s = System.currentTimeMillis();
        Map<String, String> map = convert.convertFrom(JsonConvert.TYPE_MAP_STRING_STRING, req.getParameter("bean"));
        RetResult<RandomCode> ret = null;
        UserDetail bean = new UserDetail();
        if (map.containsKey("mobile")) {
            bean.setMobile(map.get("mobile"));
            ret = service.checkRandomCode(bean.getMobile(), map.get("vercode"), RandomCode.TYPE_SMSREG);
            if (!ret.isSuccess()) {
                sendRetResult(resp, ret);
                return;
            }
        } else if (map.containsKey("email")) {
            bean.setEmail(map.get("email"));
        } else {
            bean.setAccount(map.getOrDefault("account", ""));
        }
        bean.setApptoken(map.getOrDefault("apptoken", ""));
        bean.setPassword(map.getOrDefault("password", ""));
        bean.setRegaddr(req.getRemoteAddr());
        bean.setRegagent(req.getHeader("User-Agent", ""));
        final String reqpwd = bean.getPassword();
        RetResult<UserInfo> rr = service.register(bean);
        if (rr.isSuccess()) {
            if (ret != null) {
                ret.getResult().setUserid(rr.getResult().getUserid());
                service.removeRandomCode(ret.getResult());
            }
            UserInfo curr = rr.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.isAc() ? curr.getAccount() : (curr.isMb() ? curr.getMobile() : curr.getEmail()));
            loginbean.setApptoken(bean.getApptoken());
            loginbean.setPassword(UserService.secondPasswordMD5(reqpwd));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("User-Agent"));
            if (map.containsKey("cacheday")) loginbean.setCacheday(Integer.parseInt(map.getOrDefault("cacheday", "0")));
            loginbean.setLoginip(req.getRemoteAddr());
            rr = service.login(loginbean);
        }
        long e = System.currentTimeMillis() - s;
        if (e > 500) logger.warning("/user/signup cost " + e / 1000.0 + " seconds " + bean);
        resp.finishJson(rr);
    }

    /**
     * 修改密码
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/updatepwd")
    public void updatepwd(HttpRequest req, HttpResponse resp) throws IOException {
        UserPwdBean bean = req.getJsonParameter(UserPwdBean.class, "bean");
        UserInfo curr = currentUser(req);
        if (curr != null) bean.setSessionid(req.getSessionid(false));
        RetResult<UserInfo> result = service.updatePwd(bean);
        if (result.isSuccess() && curr == null) { //找回的密码
            curr = result.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.getEmail().isEmpty() ? curr.getMobile() : curr.getEmail());
            loginbean.setPassword(UserService.secondPasswordMD5(bean.getNewpwd()));
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("User-Agent"));
            loginbean.setLoginip(req.getRemoteAddr());
            result = service.login(loginbean);
        }
        String autologin = req.getCookie(COOKIE_AUTOLOGIN);
        if (autologin != null) {
            autologin = UserService.decryptAES(autologin);
            if (autologin.contains("$0")) { //表示COOKIE_AUTOLOGIN 为密码类型存储
                String newpwd = UserService.secondPasswordMD5(bean.getNewpwd());
                int wen = autologin.indexOf('?');
                int mei = autologin.indexOf('$');
                String key = autologin.substring(0, mei + 2) + newpwd + autologin.substring(wen);
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
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
        sendRetResult(resp, result);
    }

    @WebAction(url = "/user/updateusername")
    public void updateUsername(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.updateUsername(currentUser(req).getUserid(), req.getParameter("username")));
    }

    @WebAction(url = "/user/updategender/")
    public void updateGender(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.updateGender(currentUser(req).getUserid(), Short.parseShort(req.getRequstURILastPath())));
    }

    //发送修改密码验证码
    @AuthIgnore
    @WebAction(url = "/user/smspwdcode")
    public void smscode(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSPWD, req, resp);
    }

    //发送手机修改验证码
    @AuthIgnore
    @WebAction(url = "/user/smsmobcode")
    public void smsmob(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSMOB, req, resp);
    }

    //发送手机注册验证码
    @AuthIgnore
    @WebAction(url = "/user/smsregcode")
    public void smsreg(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSREG, req, resp);
    }

    //发送手机登录验证码
    @AuthIgnore
    @WebAction(url = "/user/smslgncode")
    public void smslgn(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSLGN, req, resp);
    }

    private void smsvercode(final short type, HttpRequest req, HttpResponse resp) throws IOException {
        RetResult rr = service.smscode(type, req.getRequstURIPath("mobile:", req.getParameter("mobile")));
        if (finest) logger.finest(req.getRequestURI() + ", mobile = " + req.getParameter("mobile") + "---->" + rr);
        sendRetResult(resp, rr);
    }

    //检测账号是否有效, 返回t0表示可用.给新用户注册使用
    @AuthIgnore
    @WebAction(url = "/user/checkaccount/")
    public void checkAccount(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkAccount(req.getRequstURILastPath()));
    }

    //检测手机号码是否有效, 返回0表示可用.给新用户注册使用
    @AuthIgnore
    @WebAction(url = "/user/checkmobile/")
    public void checkMobile(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkMobile(req.getRequstURILastPath()));
    }

    //检测邮箱地址是否有效, 返回0表示可用.给新用户注册使用
    @AuthIgnore
    @WebAction(url = "/user/checkemail/")
    public void checkEmail(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkEmail(req.getRequstURILastPath()));
    }

    //验证短信验证码
    @AuthIgnore
    @WebAction(url = "/user/checkcode")
    public void checkcode(HttpRequest req, HttpResponse resp) throws IOException {
        String mobile = req.getRequstURIPath("mobile:", req.getParameter("mobile"));
        String vercode = req.getRequstURIPath("vercode:", req.getParameter("vercode"));
        RetResult<RandomCode> ret = service.checkRandomCode(mobile, vercode, (short) 0);
        sendRetcode(resp, ret.getRetcode());
    }

    //获取当前用户基本信息
    @AuthIgnore
    @WebAction(url = "/user/myinfo")
    public void myinfo(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(currentUser(req));
    }

    //获取当前用户基本信息（js格式）
    @AuthIgnore
    @WebAction(url = "/user/js/myinfo")
    public void myinfojs(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = currentUser(req);
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var userself = null;");
        } else {
            resp.finish("var userself = " + convert.convertTo(user) + ";");
        }
    }

    //获取个人基本信息
    @AuthIgnore
    @WebAction(url = "/user/info/")
    public void info(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.findUserInfo(Integer.parseInt(req.getRequstURILastPath(), 36)));
    }

    //获取当前用户详细信息
    @WebAction(url = "/user/mydetail")
    public void mydetail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish(userConvert.convertTo(service.findUserDetail(currentUser(req).getUserid())));
    }

    //获取当前用户详细信息（js格式）
    @AuthIgnore
    @WebAction(url = "/user/js/mydetail")
    public void mydetailjs(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = currentUser(req);
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var userdetailself = null;");
        } else {
            resp.finish("var userdetailself = " + userConvert.convertTo(service.findUserDetail(user.getUserid())) + ";");
        }
    }

}
