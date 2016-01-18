/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.user;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.annotation.*;
import org.redkale.convert.json.*;
import org.redkale.demo.base.*;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.service.weixin.*;
import org.redkale.util.*;

/**
 * 用户模块的Servlet
 *
 * @author zhangjx
 */
@WebServlet({"/user/*"})
public class UserServlet extends BaseServlet {

    public static final String COOKIE_AUTOLOGIN = "UNF";

    public static final String COOKIE_WXOPENID = "WXOID";

    public static final String COOKIE_QQOPENID = "QQOID";

    @Resource
    private UserService service;

    @Resource
    private WeiXinMPService wxService;

    @Resource
    private JsonConvert userConvert;

    @Override
    public void init(HttpContext context, AnyValue config) {
        JsonFactory factory = JsonFactory.root().createChild();
        factory.register(UserDetail.class, false, "mobile", "email", "wxunionid", "qqopenid", "apptoken");
        userConvert = factory.getConvert();
        super.init(context, config);
    }

    @WebAction(url = "/user/mytoken")
    public void mytoken(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish("{\"key\":\"" + req.getSessionid(false) + "\"}");
    }

    /**
     * 用户注销
     *
     * @param req
     * @param resp
     * @throws IOException
     */
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

    @AuthIgnore
    @WebAction(url = "/user/checkusername/")
    public void checkUserName(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkUsername(req.getRequstURILastPath()) ? 0 : 1010014);
    }

    /**
     * 检测邮箱地址是否有效, 返回true表示邮箱地址可用.给新用户注册使用
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/checkemail/")
    public void checkEmail(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkEmail(req.getRequstURILastPath()) ? 0 : 1010015);
    }

    @AuthIgnore
    @WebAction(url = "/user/checkmobile/")
    public void checkMobile(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.checkMobile(req.getRequstURILastPath()) ? 0 : 1010016);
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
        String openid = rr.getOrDefault("openid", "");
        if (!openid.isEmpty()) {
            HttpCookie cookie = new HttpCookie(COOKIE_WXOPENID, openid);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            resp.addCookie(cookie);
        }
        resp.setHeader("Location", req.getParameter("url", "/"));
        resp.finish(302, null);
    }

    @AuthIgnore
    @WebAction(url = "/user/qqlogin")
    public void qqlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        if (finest) logger.finest("/user/qqlogin :  " + openid + "," + access_token);
        final boolean wxbrowser = req.getHeader("User-Agent", "").contains("MicroMessenger");
        QQLoginBean bean = new QQLoginBean();
        bean.setAccesstoken(access_token);
        bean.setApptoken(req.getParameter("apptoken", ""));
        bean.setOpenid(openid);
        bean.setRegaddr(req.getRemoteAddr());
        bean.setReghost((wxbrowser ? "wx." : "") + req.getHost());
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.qqlogin(bean);
        if (rr.isSuccess()) {
            UserInfo info = rr.getResult();
            boolean emptypwd = info.getPassword().isEmpty();
            char[] chars1 = emptypwd ? (info.getQqopenid()).toCharArray() : info.getPassword().toCharArray();
            char[] chars2 = ("" + System.nanoTime()).toCharArray();
            char[] chars = new char[chars1.length + chars2.length];
            for (int i = 0; i < chars.length; i += 2) {
                chars[i] = chars1[i / 2];
                chars[i + 1] = chars2[i / 2];
            }
            String key = Integer.toString(info.getUserid(), 32) + "$" + (emptypwd ? "2" : "0") + new String(chars);
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(1000 * 24 * 60 * 60);
            resp.addCookie(cookie);
        }
        if (rr.isSuccess() && rr.getRetinfo() != null && !rr.getRetinfo().isEmpty()) {
            HttpCookie cookie = new HttpCookie(COOKIE_QQOPENID, rr.getRetinfo());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
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
     * 微信登陆
     * https://open.weixin.qq.com/connect/qrconnect?appid=wx64ae61c939bdf906&redirect_uri=xxxxx&response_type=code&scope=snsapi_login&state=wx64ae61c939bdf906_1#wechat_redirect
     * <p>
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/wxlogin")
    public void wxlogin(HttpRequest req, HttpResponse resp) throws IOException {
        String code = req.getParameter("code");
        String access_token = req.getParameter("access_token");
        String openid = req.getParameter("openid");
        String state = req.getParameter("state");  //state值格式: appid_autoregflag
        if (finest) logger.finest("/user/wxlogin :  code = " + code + ", access_token = " + access_token + ", openid = " + openid + ", state =" + state);
        int pos = state.indexOf('_');
        String appid = pos > 0 ? state.substring(0, pos) : state;
        if (appid.length() < 2) appid = "";
        boolean autoreg = (pos > 0 || "1".equals(state)) ? (state.charAt(pos + 1) == '1') : true;
        final boolean wxbrowser = req.getHeader("User-Agent", "").contains("MicroMessenger");
        WxLoginBean bean = new WxLoginBean();
        { //web方式
            bean.setAppid(appid);
            bean.setCode(code);
        }
        { //app方式
            bean.setAccesstoken(access_token);
            bean.setOpenid(openid);
        }
        bean.setAutoreg(autoreg);
        bean.setApptoken(req.getParameter("apptoken", ""));
        bean.setRegaddr(req.getRemoteAddr());
        bean.setReghost((wxbrowser ? "wx." : "") + req.getHost());
        if (autoreg) bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> rr = service.wxlogin(bean);
        if (autoreg && rr.isSuccess() && (wxbrowser || (access_token != null && !access_token.isEmpty()))) {
            UserInfo info = rr.getResult();
            boolean emptypwd = info.getPassword().isEmpty();
            char[] chars1 = emptypwd ? (info.getWxunionid()).toCharArray() : info.getPassword().toCharArray();
            char[] chars2 = ("" + System.nanoTime()).toCharArray();
            char[] chars = new char[chars1.length + chars2.length];
            for (int i = 0; i < chars.length; i += 2) {
                chars[i] = chars1[i / 2];
                chars[i + 1] = chars2[i / 2];
            }
            String key = (bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + Integer.toString(info.getUserid(), 32) + "$" + (emptypwd ? "1" : "0") + new String(chars);
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(1000 * 24 * 60 * 60);
            resp.addCookie(cookie);
        }
        if (rr.isSuccess() && rr.getRetinfo() != null && !rr.getRetinfo().isEmpty()) {
            HttpCookie cookie = new HttpCookie(COOKIE_WXOPENID, rr.getRetinfo());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
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
        bean.setLoginagent(req.getHeader("User-Agent"));
        bean.setLoginip(req.getRemoteAddr());
        String oldsessionid = req.getSessionid(false);
        if (oldsessionid != null && !oldsessionid.isEmpty()) service.logout(oldsessionid);
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> result = service.login(bean);
        if (result.isSuccess()) {
            if (bean.getCacheday() > 0) {  //保存N天 
                UserInfo info = result.getResult();
                char[] chars1 = info.getPassword().toCharArray();
                char[] chars2 = ("" + System.currentTimeMillis()).toCharArray();
                char[] chars = new char[chars1.length + chars2.length];
                for (int i = 0; i < chars.length; i += 2) {
                    chars[i] = chars1[i / 2];
                    chars[i + 1] = chars2[i / 2];
                }
                String key = (bean.emptyApptoken() ? "" : (bean.getApptoken() + "#")) + Integer.toString(info.getUserid(), 32) + "$0" + new String(chars);
                HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(bean.getCacheday() * 24 * 60 * 60);
                resp.addCookie(cookie);
            }
        }
        sendRetResult(resp, result);
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
        if (result.isSuccess() && curr == null) {
            curr = result.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.getEmail().isEmpty() ? curr.getMobile() : curr.getEmail());
            loginbean.setPassword(curr.getPassword());
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("User-Agent"));
            loginbean.setLoginip(req.getRemoteAddr());
            result = service.login(loginbean);
        }
        String autologin = req.getCookie(COOKIE_AUTOLOGIN);
        if (autologin != null) {
            char[] chars1 = curr.getPassword().toCharArray();
            char[] chars2 = ("" + System.currentTimeMillis()).toCharArray();
            char[] chars = new char[chars1.length + chars2.length];
            for (int i = 0; i < chars.length; i += 2) {
                chars[i] = chars1[i / 2];
                chars[i + 1] = chars2[i / 2];
            }
            String key = Integer.toString(curr.getUserid(), 32) + "$" + new String(chars);
            HttpCookie cookie = new HttpCookie(COOKIE_AUTOLOGIN, key);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(10000 * 24 * 60 * 60);
            resp.addCookie(cookie);
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

    @WebAction(url = "/user/updatemobile")
    public void updatemobile(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo userInfo = super.currentUser(req);
        resp.finishJson(service.updateMobile(currentUser(req).getUserid(), req.getParameter("mobile"), req.getParameter("vercode")));
    }

    @WebAction(url = "/user/findbyemail/")
    public void findbyemail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.findUserInfoByEmail(req.getRequstURILastPath().trim()));
    }

    /**
     * 忘记密码
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/forgetpwd")
    public void forgetpwd(HttpRequest req, HttpResponse resp) throws IOException {
        sendRetcode(resp, service.forgetpwd(req.getParameter("email"), req.getHost()));
    }

    @AuthIgnore
    @WebAction(url = "/user/smspwdcode")
    public void smscode(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSPWD, req, resp);
    }

    @AuthIgnore
    @WebAction(url = "/user/smsmobcode")
    public void smsmob(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSMOB, req, resp);
    }

    @AuthIgnore
    @WebAction(url = "/user/smsregcode")
    public void smsreg(HttpRequest req, HttpResponse resp) throws IOException {
        smsvercode(RandomCode.TYPE_SMSREG, req, resp);
    }

    private void smsvercode(final short type, HttpRequest req, HttpResponse resp) throws IOException {
        RetResult rr = service.smscode(type, req.getParameter("mobile"));
        if (finest) logger.finest(req.getRequestURI() + ", locale = " + req.getParameter("locale", "zh") + ", mobile = " + req.getParameter("mobile") + "---->" + rr);
        sendRetResult(resp, rr);
    }

    @AuthIgnore
    @WebAction(url = "/user/checkcode")
    public void checkcode(HttpRequest req, HttpResponse resp) throws IOException {
        RetResult<RandomCode> ret = service.checkRandomCode(req.getParameter("mobile"), req.getParameter("vercode"));
        sendRetcode(resp, ret.getRetcode());
    }

    @AuthIgnore
    @WebAction(url = "/user/regbysms")
    public void regbysms(HttpRequest req, HttpResponse resp) throws IOException {
        long s = System.currentTimeMillis();
        RetResult<RandomCode> ret = service.checkRandomCode(req.getParameter("mobile"), req.getParameter("vercode"));
        if (!ret.isSuccess()) {
            sendRetResult(resp, ret);
            return;
        }
        UserDetail bean = req.getJsonParameter(UserDetail.class, "bean");
        bean.setWxunionid("");
        bean.setRegaddr(req.getRemoteAddr());
        bean.setRegagent((req.getHeader("User-Agent", "").contains("MicroMessenger") ? "wx." : "") + req.getHost());
        String sms = "";
        RetResult<UserInfo> rr = service.register(bean);
        if (rr.isSuccess()) {
            ret.getResult().setUserid(rr.getResult().getUserid());
            service.removeRandomCode(ret.getResult());
            UserInfo curr = rr.getResult();
            LoginBean loginbean = new LoginBean();
            loginbean.setAccount(curr.getEmail().isEmpty() ? curr.getMobile() : curr.getEmail());
            loginbean.setApptoken(bean.getApptoken());
            loginbean.setPassword(curr.getPassword());
            loginbean.setSessionid(req.changeSessionid());
            loginbean.setLoginagent(req.getHeader("User-Agent"));
            loginbean.setLoginip(req.getRemoteAddr());
            rr = service.login(loginbean);
        }
        long e = System.currentTimeMillis() - s;
        if (e > 500) logger.warning("/user/regbysms cost " + e / 1000.0 + " seconds " + bean);
        resp.finishJson(rr);
    }

    /**
     * 获取个人基本信息
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/myinfo")
    public void myinfo(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(currentUser(req));
    }

    /**
     * 获取个人基本信息
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/js/myinfo")
    public void myjsinfo(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = currentUser(req);
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var userself = null;");
        } else {
            String userjson = convert.convertTo(user);
            String token = "";
            String sessionid = req.getSessionid(false);
            if (sessionid != null && !sessionid.isEmpty()) {
                token = "userself.token=\"" + sessionid + "\";var MYTOKEN=\"" + sessionid + "\";";
                if (req.getHeader("User-Agent", "").contains("MicroMessenger")) {
                    token += "userself.wxunionid=\"" + user.getWxunionid() + "\";";
                }
            }
            resp.finish("var userself=" + userjson + ";" + token);
        }
    }

    /**
     * 获取个人基本信息
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/info/")
    public void info(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.findUserInfo(Integer.parseInt(req.getRequstURILastPath(), 36)));
    }

    /**
     * 获取个人详细信息
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @AuthIgnore
    @WebAction(url = "/user/detail/")
    public void detail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.findUserDetail(Integer.parseInt(req.getRequstURILastPath(), 36)));
    }

    @WebAction(url = "/user/mydetail")
    public void mydetail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finish(userConvert.convertTo(service.findUserDetail(currentUser(req).getUserid())));
    }

    @AuthIgnore
    @WebAction(url = "/user/js/mydetail")
    public void mydetailjs(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = currentUser(req);
        resp.setContentType("application/javascript; charset=utf-8");
        if (user == null) {
            resp.finish("var current_userdetail = null;");
        } else {
            resp.finish("var current_userdetail = " + userConvert.convertTo(service.findUserDetail(user.getUserid())) + ";");
        }
    }

    /**
     * 修改用户邮箱
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    @WebAction(url = "/user/updatemail")
    public void updateEmail(HttpRequest req, HttpResponse resp) throws IOException {
        resp.finishJson(service.updateEmail(currentUser(req), req.getParameter("email"), req.getParameter("vercode")));
    }

}
