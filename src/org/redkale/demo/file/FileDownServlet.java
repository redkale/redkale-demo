/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.io.*;
import javax.annotation.Resource;
import org.redkale.demo.base.BaseServlet;
import org.redkale.net.http.*;
import org.redkale.util.AnyValue;

/**
 *
 * <pre>
 *     int   10万-100万     (36进制 4位)  255t - lflr    4-5长度  rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w?)\..*)$" /$1/$3/$2 last;            
 *     int  1000万-6000万   (36进制 5位)  5yc1t - zq0an
 *     int    2亿-20亿      (36进制 6位)  3b2ozl - x2qxvk   6-7长度  rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w{2})(\w?)\..*)$" /$1/$3/$4/$2 last;
 *    long   30亿-770亿     (36进制 7位)  1dm4etd - zdft88v
 *    long  1000亿-2万亿    (36进制 8位)  19xtf1tt - piscd0jj  8-9长度  rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w{2})(\w{2})(\w?)\..*)$" /$1/$3/$4/$5/$2 last;
 *    随机文件名:   (32进制 26位)   26-27长度    rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w?)\..*)$" /$1/$3/$4/$5/$6/$7/$8/$9/$10/$11/$12/$13/$14/$2 last;
 * </pre>
 *
 * 所有静态资源的请求url的根目录为dir，便于nginx进行静动分离
 * 请求url为 /dir/{分类目录}/文件名
 *
 * @author zhangjx
 */
@WebServlet(value = {"/dir/*"}, repair = false)
public class FileDownServlet extends BaseServlet {

    @Resource(name = "APP_HOME")
    private File home;

    private File files;

    private File dface;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
        this.files = new File(home, "files");
        this.files.mkdirs();
        this.dface = new File(files, "/face/my.jpg"); //默认头像
    }

    @AuthIgnore
    @WebAction(url = "/dir/")
    public void dir(HttpRequest req, HttpResponse resp) throws IOException {
        download(req, resp);
    }

    private void download(HttpRequest req, HttpResponse resp) throws IOException {
        final String uri = req.getRequestURI().substring(req.getRequestURI().indexOf("/dir") + 4);
        boolean face = uri.startsWith("/face");
        resp.setHeader("Cache-Control", "max-age=3600");
        int pos = uri.lastIndexOf('/');
        File f = new File(files, uri.substring(0, pos + 1) + FileService.hashPath(uri.substring(pos + 1)) + (face ? ".jpg" : ""));
        if (!f.isFile() && face) f = dface;
        if (f.isFile()) {
            resp.finish(req.getParameter("filename"), f);
        } else {
            resp.finish(404, null);
        }
    }

}
