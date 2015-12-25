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
import org.redkale.net.*;
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

    @Resource
    protected JsonConvert convert;

    //protected static httl.Engine engine;
    @Resource(name = "APP_HOME")
    private File home;

    @Resource(name = "APP_ADDR")
    private String nodeAddress;

    @Resource
    private UserService service;

    @Override
    public void init(Context context, AnyValue config) {
        super.init(context, config);
    }

    @Override
    public boolean preExecute(final HttpRequest request, final HttpResponse response) throws IOException {
        if (finer) response.setRecycleListener(() -> {  //记录处理时间比较长的请求
                long e = System.currentTimeMillis() - request.getCreatetime();
                if (e > 200) logger.finer("http-execute-cost-time: " + e + " ms. request = " + request);
            });
        return true;
    }

    @Override
    public final boolean authenticate(int module, int actionid, HttpRequest request, HttpResponse response) throws IOException {
        UserInfo info = currentUser(request);
        if (info != null) return true;
        if (info == null) {
            response.addHeader("retcode", "1010001");
            response.addHeader("retmessage", "Not Login");
            response.setStatus(203);
            response.finish("{'success':false, 'message':'Not Login'}");
        } else {
            response.addHeader("retcode", "1010030");
            response.addHeader("retmessage", "No Authority");
            response.setStatus(203);
            response.finish("{'success':false, 'message':'No Authority'}");
        }
        return false;
    }

    protected final UserInfo currentUser(HttpRequest req) throws IOException {
        return currentUser(service, req);
    }

    public static final UserInfo currentUser(UserService service, HttpRequest req) throws IOException {
        UserInfo user = (UserInfo) req.getAttribute("CurrentUserInfo");
        if (user != null) return user;
        String sessionid = req.getSessionid(false);
        if (sessionid == null || sessionid.isEmpty()) sessionid = req.getParameter("token");
        if (sessionid != null && !sessionid.isEmpty()) user = service.current(sessionid);
        if (user != null) {
            req.setAttribute("CurrentUserInfo", user);
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
        if (result.isSuccess()) {
            req.setAttribute("CurrentUserInfo", user);
        }
        return user;
    }

    protected void sendJsResult(HttpResponse resp, String var, Object result) {
        resp.setContentType("application/javascript; charset=utf-8");
        resp.finish("var " + var + " = " + convert.convertTo(result) + ";");
    }

    protected void sendRetResult(HttpResponse resp, RetResult ret) {
        if (!ret.isSuccess()) resp.addHeader("retcode", ret.getRetcode());
        resp.finishJson(ret);
    }

    protected void sendRetcode(HttpResponse resp, int retcode) {
        if (retcode != 0) resp.addHeader("retcode", retcode);
        resp.finish("{\"retcode\":" + retcode + ", \"success\": " + (retcode == 0) + "}");
    }

    protected Flipper findFlipper(HttpRequest request) {
        Flipper flipper = request.getJsonParameter(Flipper.class, "flipper");
        if (flipper == null) flipper = new Flipper();
        if (flipper.getSize() > 20) flipper.setSize(20);
        return flipper;
    }

    protected final long referFirst36id(HttpRequest req) throws IOException {
        try {
            String[] refs = refer(req.getHeader("Referer", ""));
            if (refs.length < 1) return 0;
            return Long.parseLong(refs[0], 36);
        } catch (Exception e) {
            logger.log(Level.FINEST, "referFirst36id : " + req, e);
            return 0;
        }
    }

    protected final long referLast36id(HttpRequest req) throws IOException {
        try {
            String id = req.getParameter("id");
            if (id != null) return Long.parseLong(id, 36);
            String[] refs = refer(req.getHeader("Referer", ""));
            if (refs.length < 1) return 0;
            return Long.parseLong(refs[refs.length - 1], 36);
        } catch (Exception e) {
            logger.log(Level.FINEST, "referLast36id : " + req, e);
            return 0;
        }
    }

    protected final String[] refer(HttpRequest req) throws IOException {
        try {
            String id = req.getParameter("id");
            if (id != null) return new String[]{id};
            return refer(req.getHeader("Referer", ""));
        } catch (Exception e) {
            logger.log(Level.FINEST, "referLast36id : " + req, e);
            return new String[0];
        }
    }

    private static String[] refer(String referer) {
        int pos = referer.indexOf('?');
        if (pos > 0) referer = referer.substring(0, pos);
        pos = referer.indexOf('#');
        if (pos > 0) referer = referer.substring(0, pos);
        int start = referer.indexOf('-');
        if (start < 0) return new String[0];
        int end = referer.lastIndexOf('.');
        if (end < 0) end = referer.length() - 1;
        return referer.substring(start + 1, end).split("-");
    }
}
