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
import org.redkale.source.*;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public class BaseServlet extends org.redkale.net.http.BasedHttpServlet {

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
            sendRetResult(response, RET_UNLOGIN);
            return false;
        } else if (!info.checkAuth(module, actionid)) {
            sendRetResult(response, RET_AUTHILLEGAL);
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

    /**
     * 将对象以js方式输出
     *
     * @param resp   HTTP响应对象
     * @param var    对象名
     * @param result 对象
     */
    protected void sendJsResult(HttpResponse resp, String var, Object result) {
        resp.setContentType("application/javascript; charset=utf-8");
        resp.finish("var " + var + " = " + convert.convertTo(result) + ";");
    }

    /**
     * 将对象以js方式输出
     *
     * @param resp        HTTP响应对象
     * @param jsonConvert convert对象
     * @param var         对象名
     * @param result      对象
     */
    protected void sendJsResult(HttpResponse resp, JsonConvert jsonConvert, String var, Object result) {
        resp.setContentType("application/javascript; charset=utf-8");
        resp.finish("var " + var + " = " + jsonConvert.convertTo(result) + ";");
    }

    /**
     * 将结果对象输出， 异常的结果在HTTP的header添加retcode值
     *
     * @param resp HTTP响应对象
     * @param ret  结果对象
     */
    protected void sendRetResult(HttpResponse resp, RetResult ret) {
        if (!ret.isSuccess()) {
            resp.addHeader("retcode", ret.getRetcode());
            resp.addHeader("retinfo", ret.getRetinfo());
        }
        resp.finishJson(ret);
    }

    /**
     * 将结果对象输出， 异常的结果在HTTP的header添加retcode值
     *
     * @param resp        HTTP响应对象
     * @param jsonConvert convert对象
     * @param ret         结果对象
     */
    protected void sendRetResult(HttpResponse resp, JsonConvert jsonConvert, RetResult ret) {
        if (!ret.isSuccess()) {
            resp.addHeader("retcode", ret.getRetcode());
            resp.addHeader("retinfo", ret.getRetinfo());
        }
        resp.finishJson(jsonConvert, ret);
    }

    /**
     * 将结果对象输出， 异常的结果在HTTP的header添加retcode值
     *
     * @param resp    HTTP响应对象
     * @param retcode 结果码
     */
    protected void sendRetcode(HttpResponse resp, int retcode) {
        if (retcode != 0) resp.addHeader("retcode", retcode);
        resp.finish("{\"retcode\":" + retcode + ", \"success\": " + (retcode == 0) + "}");
    }

    /**
     * 将结果对象输出， 异常的结果在HTTP的header添加retcode值
     *
     * @param resp    HTTP响应对象
     * @param retcode 结果码
     * @param retinfo 结果信息
     */
    protected void sendRetcode(HttpResponse resp, int retcode, String retinfo) {
        if (retcode != 0) resp.addHeader("retcode", retcode);
        if (retinfo != null && !retinfo.isEmpty()) resp.addHeader("retinfo", retinfo);
        resp.finish("{\"retcode\":" + retcode + ", \"success\": " + (retcode == 0) + "}");
    }

    /**
     * 获取翻页对象 http://demo.redkale.org/pipes/records/list/offset:0/limit:20  <br>
     * http://demo.redkale.org/pipes/records/list?flipper={'offset':0,'limit':20}  <br>
     * 以上两种接口都可以获取到翻页对象
     *
     * @param request HTTP请求对象
     *
     * @return
     */
    protected Flipper findFlipper(HttpRequest request) {
        return findFlipper(request, 0);
    }

    protected Flipper findFlipper(HttpRequest request, int defaultLimit) {
        Flipper flipper = request.getJsonParameter(Flipper.class, "flipper");
        if (flipper == null) {
            int limit = request.getRequstURIPath("limit:", defaultLimit);
            int offset = request.getRequstURIPath("offset:", 0);
            if (limit > 0) flipper = new Flipper(limit, offset);
        }
        if (flipper == null) flipper = defaultLimit > 0 ? new Flipper(defaultLimit) : new Flipper();
        return flipper;
    }

}
