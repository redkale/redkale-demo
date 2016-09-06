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
 *     int   10万-100万     (36进制 4位)  255t - lflr    长度4  rewrite "^/dir/(\w+)/((\w{2})(\w{2})\..*)$" /$1/$3/$2 last;
 *     int  1000万-6000万   (36进制 5位)  5yc1t - zq0an   长度5-6   rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w\w?)\..*)$" /$1/$3/$4/$2 last;
 *     int    2亿-20亿      (36进制 6位)  3b2ozl - x2qxvk
 *    long   30亿-770亿     (36进制 7位)  1dm4etd - zdft88v   长度7-8   rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w{2})(\w\w?)\..*)$" /$1/$3/$4/$5/$2 last;
 *    long  1000亿-2万亿    (36进制 8位)  19xtf1tt - piscd0jj
 *    随机文件名:   (32进制 26位)   26-27长度
 *      #文件名 长度: 26 (1)
 *      rewrite "^/dir/(\w+)/((\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{14})\..*)$" /dir/$1/$3/$4/$5/$6/$7/$8/$2;
 *      #文件名 长度: 26 (2)
 *      rewrite "^/dir/(\w+)/(\w\w/\w\w/\w\w/\w\w/\w\w/\w\w)/(\w{12}(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})(\w{2})\..*)$" /$1/$2/$4/$5/$6/$7/$8/$9/$3 last;
 *
 * </pre>
 *
 * 所有静态资源的请求url的根目录为dir，便于nginx进行静动分离
 * 请求url为 /dir/{分类目录}/文件名
 *
 * @author zhangjx
 */
@WebServlet(value = {"/dir/*"}, repair = false, comment = "仅供开发阶段使用")
public class FileDownServlet extends BaseServlet {

    @Resource(name = "APP_HOME")
    private File home;

    private File files;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
        this.files = new File(home, "files");
        this.files.mkdirs();
    }

    @AuthIgnore
    @WebAction(url = "/dir/")
    public void dir(HttpRequest req, HttpResponse resp) throws IOException {
        download(req, resp);
    }

    private void download(HttpRequest req, HttpResponse resp) throws IOException {
        final String uri = req.getRequestURI().substring(req.getRequestURI().indexOf("/dir") + 4);
        resp.setHeader("Cache-Control", "max-age=3600");
        int pos = uri.lastIndexOf('/');
        File f = new File(files, uri.substring(0, pos + 1) + FileService.hashPath(uri.substring(pos + 1)));
        if (!f.isFile()) f = new File(files, uri.substring(0, pos + 1) + "def.jpg"); //每个目录下放个默认图片
        if (f.isFile()) {
            resp.finish(req.getParameter("filename"), f);
        } else {
            resp.finish(404, null);
        }
    }

}
