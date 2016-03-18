/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import com.sun.image.codec.jpeg.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import org.redkale.demo.base.*;
import org.redkale.net.http.*;
import org.redkale.util.AnyValue;

/**
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
        upload(req, resp, 10 * 1024 * 1024L);
    }

    protected void upload(HttpRequest req, HttpResponse resp, long max) throws IOException {
        upload(req, resp, req.getRequstURILastPath(), 0, true, max);
    }

    protected void upload(HttpRequest req, HttpResponse resp, String dir, int size, long max) throws IOException {
        upload(req, resp, dir, size, true, max);
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
                service.storeFace(user.getUserid(), img.getType(), img);
            }
            resp.finish("{\"success\":true,\"retcode\":0,\"fileid\":\"" + user.getUser36id() + ".jpg\"}");
            return;
        }
        resp.finish("{\"success\":false,\"retcode\":2010001,\"retinfo\":\"no upload file entry\"}");
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

    protected void upload(HttpRequest req, HttpResponse resp, String dir, final int size, final boolean sync, final long max) throws IOException {
        upload(req, resp, dir, size, 0, sync, max);
    }

    protected void upload(final HttpRequest req, HttpResponse resp, String dir, final int size, final float rate, final boolean sync, final long max) throws IOException {
        String fileid = "";
        File file = null;
        for (MultiPart part : req.multiParts()) {
            final String mime = MimeType.getByFilename(part.getFilename());
            if (mime.contains("image/")) { //需要剪切图片                
                byte[] bytes = part.getContentBytes(max);
                if (bytes != null) {
                    final boolean jpeg = bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[bytes.length - 2] == 0xFF && bytes[bytes.length - 1] == 0xD9;
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (rate > 0) {
                        int nheight = (int) (image.getWidth() / rate);
                        if (image.getHeight() > nheight) { //太高了
                            image = image.getSubimage(0, 0, image.getWidth(), nheight);
                        } else if (image.getHeight() < nheight) { //太宽了
                            int nwidth = (int) (image.getHeight() * rate);
                            image = image.getSubimage((image.getWidth() - nwidth) / 2, 0, nwidth, image.getHeight());
                        }
                    }
                    final boolean large = image.getWidth() > 2048;
                    if (!jpeg || size > 0 || large) {
                        int w = size > 0 ? size : (large ? 2048 : image.getWidth());
                        int h = size > 0 ? size : (large ? image.getHeight() * 2048 / image.getWidth() : image.getHeight());
                        if (rate > 0) h = (int) (w / rate);
                        BufferedImage target = new BufferedImage(w, h, jpeg ? image.getType() : BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = target.createGraphics();
                        if (!jpeg) {
                            // 因为有的图片背景是透明色，所以用白色填充 FIXED
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1));
                            g.fillRect(0, 0, w, h);
                        }
                        g.drawImage(image.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, w, h, null);
                        g.dispose();
                        image = target;
                    }
                    file = service.createFile(dir, null, "jpg");
                    FileOutputStream out = new FileOutputStream(file);
                    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                    JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
                    encoder.setJPEGEncodeParam(param);
                    encoder.encode(image);
                    out.close();
                }
            } else {
                file = service.storeFile(sync, dir, null, part.getFilename(), max, part.getInputStream());
            }
            if (file != null) fileid = file.getName();
            if (fine) {
                final String fid = fileid;
                final long s = System.currentTimeMillis() - req.getCreatetime();
                if (s >= 1000) resp.setRecycleListener((req0, resp0) -> logger.finer("upload-cost-time " + s / 1000.0 + " seconds, request = " + req + ", limit = " + max / (1024 * 1024) + "M, fileid = " + fid));
            }
            if (fileid.isEmpty()) {
                resp.finish("{\"success\":false,\"retcode\":2010001,\"retinfo\":\"file too long or io error\"}");
            } else {
                resp.finish("{\"success\":true,\"retcode\":0,\"fileid\":\"" + fileid + "\",\"filename\":\"" + part.getFilename() + "\",\"filelength\":" + part.getReceived() + "}");
            }
            return;
        }
        resp.finish("{\"success\":false,\"retcode\":2010001,\"retinfo\":\"no upload file entry\"}");
    }

}
