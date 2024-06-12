/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.io.IOException;
import java.util.logging.*;
import org.redkale.annotation.Resource;
import org.redkale.convert.json.JsonConvert;
import org.redkale.demo.user.*;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.AnyValue;

/**
 *
 * @author zhangjx
 */
public class BaseServlet extends HttpServlet {

    protected static final boolean winos = System.getProperty("os.name").contains("Window");

    protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean fine = logger.isLoggable(Level.FINE);

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);

    protected static final RetResult RET_UNLOGIN = DemoRetCodes.retResult(DemoRetCodes.RET_USER_UNLOGIN);

    protected static final RetResult RET_AUTHILLEGAL = DemoRetCodes.retResult(DemoRetCodes.RET_USER_AUTH_ILLEGAL);

    @Resource
    protected JsonConvert convert;

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
     * @throws IOException
     */
    @Override
    public void preExecute(final HttpRequest request, final HttpResponse response) throws IOException {
        if (finer) {
            response.recycleListener((req, resp) -> {  //记录处理时间比较长的请求
                long e = System.currentTimeMillis() - ((HttpRequest) req).getCreateTime();
                if (e > 200) {
                    logger.finer("http-execute-cost-time: " + e + " ms. request = " + req);
                }
            });
        }
        request.setCurrentUserid(currentUserid(service, request));
        response.nextEvent();
    }

    /**
     * 校验用户的登录态
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     *
     * @throws IOException
     */
    @Override
    public void authenticate(HttpRequest request, HttpResponse response) throws IOException {
        int userid = request.currentUserid(int.class);
        if (userid == 0) {
            response.finishJson(RET_UNLOGIN);
            return;
        }
        response.nextEvent();
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
    public static final int currentUserid(UserService service, HttpRequest req) throws IOException {
        int userid = req.currentUserid(int.class);
        if (userid > 0) {
            return userid;
        }
        UserInfo user = null;
        String sessionid = req.getSessionid(false);
        if (sessionid == null || sessionid.isEmpty()) {
            sessionid = req.getParameter("token");
        }
        if (sessionid != null && !sessionid.isEmpty()) {
            user = service.current(sessionid);
        }
        if (user != null) {
            return user.getUserid();
        }
        String autologin = req.getCookie(UserServlet.COOKIE_AUTOLOGIN);
        if (autologin == null) {
            return 0;
        }
        autologin = autologin.replace('"', ' ').trim();
        LoginBean bean = new LoginBean();
        bean.setCookieInfo(autologin);
        bean.setLoginAgent(req.getHeader("User-Agent"));
        bean.setLoginIp(req.getRemoteAddr());
        bean.setSessionid(req.changeSessionid());
        RetResult<UserInfo> result = service.login(bean);
        user = result.getResult();
        return user == null ? 0 : user.getUserid();
    }

}
