/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.io.*;
import org.redkale.annotation.Resource;
import static org.redkale.demo.base.RetCodes.*;
import org.redkale.demo.base.*;
import org.redkale.demo.user.UserService;
import org.redkale.net.http.*;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 * 
 *
 * @author zhangjx
 */
@WebServlet(value = {"/upload/*"}, comment = "资源上传服务")
public class FileUploadServlet extends BaseServlet {

    @Resource
    protected FileService service;

    @Resource
    protected UserService userService;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
    }

    @HttpMapping(url = "/upload/filesroot", comment = "获取资源路径，仅供内部系统使用") // 
    public void filesroot(HttpRequest req, HttpResponse resp) throws IOException {
        String path = "";
        //10.0.0.0/8：10.0.0.0～10.255.255.255 
        //172.16.0.0/12：172.16.0.0～172.31.255.255 
        //192.168.0.0/16：192.168.0.0～192.168.255.255
        String ip = req.getRemoteAddr();
        if ("127.0.0.1".equals(ip) || ip.startsWith("10.") || ip.startsWith("192.168.")
            || ip.startsWith("172.16.") || ip.startsWith("172.17.")
            || ip.startsWith("172.18.") || ip.startsWith("172.19.")
            || ip.startsWith("172.20.") || ip.startsWith("172.21.")
            || ip.startsWith("172.22.") || ip.startsWith("172.23.")
            || ip.startsWith("172.24.") || ip.startsWith("172.25.")
            || ip.startsWith("172.26.") || ip.startsWith("172.27.")
            || ip.startsWith("172.28.") || ip.startsWith("172.29.")
            || ip.startsWith("172.30.") || ip.startsWith("172.31.")) { //只能局域网访问
            path = service.getFilespath();
        }
        resp.finish(path);
    }

    @HttpMapping(url = "/upload/face", auth = true, comment = "上传头像 以正方形规格存储") // 
    public void face(HttpRequest req, HttpResponse resp) throws IOException {
        int userid = req.currentIntUserid();
        uploadImg(req, resp, "face", Integer.toString(userid, 36), FileService.face_widths, ImageRatio.RATIO_1_1, 5 * 1024 * 1024L);
    }

    @HttpMapping(url = "/upload/image", auth = true, comment = "上传图片") // 
    public void image(HttpRequest req, HttpResponse resp) throws IOException {
        uploadImg(req, resp, "image", null, null, null, 5 * 1024 * 1024L);
    }

    @HttpMapping(url = "/upload/file", auth = true, comment = "上传附件") // 
    public void file(HttpRequest req, HttpResponse resp) throws IOException {
        uploadBin(req, resp, "file", null, 50 * 1024 * 1024L);
    }

    @HttpMapping(url = "/upload/video", auth = true, comment = "上传视频") // 
    public void video(HttpRequest req, HttpResponse resp) throws IOException {
        uploadBin(req, resp, "video", null, 50 * 1024 * 1024L);
    }

    @HttpMapping(url = "/upload/music", auth = true, comment = "上传音乐") // 
    public void music(HttpRequest req, HttpResponse resp) throws IOException {
        uploadBin(req, resp, "music", null, 50 * 1024 * 1024L);
    }

    protected void uploadBin(HttpRequest req, HttpResponse resp, long max) throws IOException {
        uploadBin(req, resp, req.getRequstURILastPath(), null, max);
    }

    protected void uploadBin(HttpRequest req, HttpResponse resp, String dir, long max) throws IOException {
        uploadBin(req, resp, dir, null, max);
    }

    protected void uploadImg(HttpRequest req, HttpResponse resp, int[] widths, final ImageRatio ratio, long max) throws IOException {
        uploadImg(req, resp, req.getRequstURILastPath(), null, widths, ratio, true, max);
    }

    protected void uploadImg(HttpRequest req, HttpResponse resp, String dir, int[] widths, final ImageRatio ratio, long max) throws IOException {
        uploadImg(req, resp, dir, null, widths, ratio, true, max);
    }

    protected void uploadImg(HttpRequest req, HttpResponse resp, String dir, String fileid0, int[] widths, final ImageRatio ratio, long max) throws IOException {
        uploadImg(req, resp, dir, fileid0, widths, ratio, true, max);
    }

    protected void uploadImg(HttpRequest req, HttpResponse resp, String dir, String fileid0, int[] widths, final ImageRatio ratio, long max, Runnable runner) throws IOException {
        uploadImg(req, resp, dir, fileid0, widths, ratio, true, max, runner);
    }

    protected void uploadImg(final HttpRequest req, HttpResponse resp, String dir, String fileid0, int[] widths, final ImageRatio ratio, final boolean sync, final long max) throws IOException {
        uploadImg(req, resp, dir, fileid0, widths, ratio, sync, max, null);
    }

    protected void uploadImg(final HttpRequest req, HttpResponse resp, String dir, String fileid0, int[] widths, final ImageRatio ratio, final boolean sync, final long max, Runnable runner) throws IOException {
        for (MultiPart part : req.multiParts()) {
            final String mime = MimeType.getByFilename(part.getFilename());
            if (!mime.contains("image/")) { //不是图片
                resp.finishJson(RetCodes.retResult(RET_UPLOAD_NOTIMAGE));
                return;
            }
            String fileid = service.storeMultiJPGFile(dir, fileid0, part.getFilename(), widths, ratio, part.getContentBytes(max), runner);
            if (fileid == null) {
                resp.finishJson(RetCodes.retResult(RET_UPLOAD_NOTIMAGE));
            } else if (fileid.isEmpty()) {
                resp.finishJson(RetCodes.retResult(RET_UPLOAD_FILETOOBIG));
            } else {
                FileUploadItem item = new FileUploadItem(fileid, part.getFilename(), part.getReceived());
                resp.finishJson(RetResult.success(Utility.ofList(item)));
            }
            return;
        }
        resp.finishJson(RetCodes.retResult(RET_UPLOAD_NOFILE));
    }

    protected void uploadBin(final HttpRequest req, HttpResponse resp, String dir, String fileid0, final long max) throws IOException {
        String fileid = "";
        File file;
        for (MultiPart part : req.multiParts()) {
            file = service.storeFile(dir, fileid0, part.getFilename(), max, part.getInputStream());
            if (file != null) fileid = file.getName();
            if (file == null || fileid.isEmpty()) {
                resp.finishJson(RetCodes.retResult(RET_UPLOAD_FILETOOBIG));
            } else {
                FileUploadItem item = new FileUploadItem(fileid, part.getFilename(), part.getReceived());
                resp.finishJson(RetResult.success(Utility.ofList(item)));
            }
            return;
        }
        resp.finishJson(RetCodes.retResult(RET_UPLOAD_NOFILE));
    }

}
