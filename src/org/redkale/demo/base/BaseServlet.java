/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.io.*;
import java.util.logging.*;
import javax.annotation.*;
import org.redkale.convert.json.*;
import org.redkale.demo.user.*;
import org.redkale.net.http.*;
import org.redkale.service.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class BaseServlet extends org.redkale.net.http.HttpBaseServlet {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean fine = logger.isLoggable(Level.FINE);

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

    protected static final RetResult RET_UNLOGIN = RetCodes.retResult(RetCodes.RET_USER_UNLOGIN);

    protected static final RetResult RET_AUTHILLEGAL = RetCodes.retResult(RetCodes.RET_USER_AUTH_ILLEGAL);

    @Resource
    protected JsonConvert convert;

    //protected static httl.Engine engine;
    @Resource
    private UserService service;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
    }

    /**
     * Servlet的入口判断，一般用于全局的基本校验和预处理
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     *
     * @return
     * @throws IOException
     */
    @Override
    public boolean preExecute(final HttpRequest request, final HttpResponse response) throws IOException {
        if (finer) response.setRecycleListener((req, resp) -> {  //记录处理时间比较长的请求
                long e = System.currentTimeMillis() - ((HttpRequest) req).getCreatetime();
                if (e > 200) logger.finer("http-execute-cost-time: " + e + " ms. request = " + req);
            });
        return true;
    }

    /**
     * 校验用户的登录态
     *
     * @param module   模块ID，为0通常无需判断
     * @param actionid 操作ID，为0通常无需判断
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     *
     * @return
     * @throws IOException
     */
    @Override
    public final boolean authenticate(int module, int actionid, HttpRequest request, HttpResponse response) throws IOException {
        UserInfo info = currentUser(request);
        if (info == null) {
            response.finishJson(RET_UNLOGIN);
            return false;
        } else if (!info.checkAuth(module, actionid)) {
            response.finishJson(RET_AUTHILLEGAL);
            return false;
        }
        return true;
    }

    /**
     * 获取当前用户对象，没有返回null
     *
     * @param req HTTP请求对象
     *
     * @return
     * @throws IOException
     */
    protected final UserInfo currentUser(HttpRequest req) throws IOException {
        return currentUser(service, req);
    }

    /**
     * 获取当前用户对象，没有返回null, 提供static方法便于WebSocket进行用户态判断
     *
     * @param service UserService
     * @param req     HTTP请求对象
     *
     * @return
     * @throws IOException
     */
    public static final UserInfo currentUser(UserService service, HttpRequest req) throws IOException {
        UserInfo user = (UserInfo) req.getAttribute("$_CURRENT_USER");
        if (user != null) return user;
        String sessionid = req.getSessionid(false);
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("token");
        if (sessionid != null && !sessionid.isEmpty()) user = service.current(sessionid);
        if (user != null) {
            req.setAttribute("$_CURRENT_USER", user);
            return user;
        }
        String autologin = req.getCookie(UserServlet.COOKIE_AUTOLOGIN);
        if (autologin == null) return null;
        autologin = autologin.replace('"', ' ').trim();
        LoginBean bean = new LoginBean();
        bean.setCookieinfo(autologin);
        bean.setLoginagent(req.getHeader("User-Agent"));
        bean.setLoginip(req.getRemoteAddr());
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> result = service.login(bean);
        user = result.getResult();
        if (result.isSuccess()) req.setAttribute("$_CURRENT_USER", user);
        return user;
    }

}
