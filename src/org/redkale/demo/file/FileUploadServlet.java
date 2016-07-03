/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import static org.redkale.demo.base.RetCodes.*;
import org.redkale.demo.base.*;
import org.redkale.net.http.*;
import org.redkale.util.AnyValue;

/**
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
 * @author zhangjx
 */
@WebServlet({"/upload/*"})
public class FileUploadServlet extends BaseServlet {

    private static Font FONT = new Font("微软雅黑", Font.ITALIC, 12);

    private static final Color COLOR = Color.WHITE;

    private static final Color FONT_COLOR = new Color(255, 255, 255, 150);

    private static final Color FONT_SHADOW_COLOR = new Color(170, 170, 170, 77);

    @Resource
    protected FileService service;

    @Override
    public void init(HttpContext context, AnyValue config) {
        super.init(context, config);
    }

    @WebAction(url = "/upload/fres")  //上传附件
    public void tres(HttpRequest req, HttpResponse resp) throws IOException {
        uploadBin(req, resp, 10 * 1024 * 1024L);
    }

    @WebAction(url = "/upload/face") // 上传头像 以正方形规格存储
    public void face(HttpRequest req, HttpResponse resp) throws IOException {
        UserInfo user = currentUser(req);
        for (MultiPart part : req.multiParts()) {
            byte[] byts = part.getContentBytes(10 * 1024 * 1024L);
            if (byts == null) {
                resp.finish("{\"success\":false,\"retcode\":2010001,\"retinfo\":\"file too long or io error\"}");
            } else {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(byts));
                service.storeFace(user.getUserid(), img);
            }
            resp.finish("{\"success\":true,\"retcode\":0,\"fileid\":\"" + user.getUser36id() + ".jpg\"}");
            return;
        }
        resp.finish("{\"success\":false,\"retcode\":2010001,\"retinfo\":\"no upload file entry\"}");
    }

    protected void uploadBin(HttpRequest req, HttpResponse resp, long max) throws IOException {
        uploadBin(req, resp, req.getRequstURILastPath(), null, true, max);
    }

    protected void uploadBin(HttpRequest req, HttpResponse resp, String dir, long max) throws IOException {
        uploadBin(req, resp, dir, null, true, max);
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

    protected void uploadImg(final HttpRequest req, HttpResponse resp, String dir, String fileid0, int[] widths, final ImageRatio ratio, final boolean sync, final long max) throws IOException {
        for (MultiPart part : req.multiParts()) {
            final String mime = MimeType.getByFilename(part.getFilename());
            if (!mime.contains("image/")) { //不是图片
                sendRetResult(resp, RetCodes.retResult(RET_UPLOAD_NOTIMAGE));
                return;
            }
            String fileid = service.storeMultiJPGFile(dir, fileid0, widths, ratio, part.getContentBytes(max), null);
            if (fileid.isEmpty()) {
                sendRetResult(resp, RetCodes.retResult(RET_UPLOAD_FILETOOBIG));
            } else {
                resp.finish("{\"success\":true,\"retcode\":0,\"fileid\":\"" + fileid + "\",\"filename\":\"" + part.getFilename() + "\",\"filelength\":" + part.getReceived() + "}");
            }
            return;
        }
        sendRetResult(resp, RetCodes.retResult(RET_UPLOAD_NOFILE));
    }

    protected void uploadBin(final HttpRequest req, HttpResponse resp, String dir, String fileid0, final boolean sync, final long max) throws IOException {
        String fileid = "";
        File file;
        for (MultiPart part : req.multiParts()) {
            file = service.storeFile(sync, dir, fileid0, part.getFilename(), max, part.getInputStream());
            if (file != null) fileid = file.getName();
            if (fileid.isEmpty()) {
                sendRetResult(resp, RetCodes.retResult(RET_UPLOAD_FILETOOBIG));
            } else {
                resp.finish("{\"success\":true,\"retcode\":0,\"fileid\":\"" + fileid + "\",\"filename\":\"" + part.getFilename() + "\",\"filelength\":" + part.getReceived() + "}");
            }
            return;
        }
        sendRetResult(resp, RetCodes.retResult(RET_UPLOAD_NOFILE));
    }

    /**
     * 水印
     * <p>
     * @param image
     * @param font
     * @param fontColor
     * @param texts
     */
    protected static void makeWaterMark(BufferedImage image, Font font, Color fontColor, String... texts) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (font == null) font = FONT;
        graphics.setFont(FONT);
        if (fontColor == null) fontColor = COLOR;
        graphics.setColor(fontColor);
        for (int i = 0; i < texts.length; i++) {
            if (texts[i] == null || texts[i].trim().isEmpty()) continue;
            FontRenderContext context = graphics.getFontRenderContext();
            Rectangle2D fontRectangle = font.getStringBounds(texts[i], context);
            int sw = (int) fontRectangle.getWidth();
            int sh = (int) fontRectangle.getHeight();
            if (texts.length - i == 1) {
                graphics.drawString(texts[i], image.getWidth() - sw - 6, image.getHeight() - 8);
            } else {
                graphics.drawString(texts[i], image.getWidth() - sw - 6, image.getHeight() - sh * (texts.length - 1) - 8);
            }
        }
        graphics.dispose();
    }
}
