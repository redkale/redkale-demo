/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.io.*;
import javax.annotation.*;
import org.redkale.demo.base.*;
import org.redkale.net.*;
import org.redkale.net.http.*;
import org.redkale.util.*;

/**
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
    public void init(Context context, AnyValue config) {
        super.init(context, config);
        this.files = new File(home, "files");
        this.files.mkdirs();
        this.dface = new File(files, "/face/my.jpg");
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
            resp.finish(f);
        } else {
            resp.finish(404, null);
        }
    }

}
