/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;
import java.security.SecureRandom;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import org.redkale.demo.base.BaseService;
import org.redkale.util.*;

/**
 * 文件
 *
 * @author zhangjx
 */
public class FileService extends BaseService {

    private static final String format = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%tL";

    static final int[] goods_widths = {800};

    //files 目录
    private File files;

    //自定义的文件根目录，没有设置默认使用 {home}/files 值
    @Resource(name = "property.files.root")
    private String filesroot;

    @Resource(name = "APP_HOME")
    private File home;

    @Override
    public void init(AnyValue config) {
        initPath();
        if (finest) logger.finest("files.root= " + this.files.getPath());
    }

    //初始化静态资源的总文件目录
    private void initPath() {
        if (this.files != null) return;
        String fr = this.filesroot;
        if (fr != null) fr = fr.trim();
        if (fr != null && fr.toLowerCase().startsWith("http") && fr.indexOf(':') > 0) {
            try {
                fr = Utility.getHttpContent(fr);
            } catch (IOException e) {
                logger.log(Level.WARNING, "files.root[" + fr + "] is illegal", e);
                fr = null;
            }
        }
        this.files = (fr == null || fr.isEmpty()) ? new File(home, "files") : new File(fr);
        this.files.mkdirs();
    }

    public String getFilespath() throws IOException {
        return this.files.getCanonicalPath();
    }

    @Override
    public void destroy(AnyValue config) {
    }

    static final int[] face_widths = {64, 128, 512}; //头像有三种规格: 64*64、128*128 512*512

    public final String storeFace(int userid, BufferedImage srcImage) throws IOException {
        return storeMultiJPGFile("face", Integer.toString(userid, 36), face_widths, ImageRatio.RATIO_1_1, srcImage, null);
    }

    public final String storeMultiJPGFile(final String dir, final String fileid, int[] widths, final ImageRatio ratio, byte[] bytes, Runnable runner) throws IOException {
        if (bytes == null) return "";
        final boolean jpeg = bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[bytes.length - 2] == 0xFF && bytes[bytes.length - 1] == 0xD9;
        BufferedImage image = ratio == null ? ImageIO.read(new ByteArrayInputStream(bytes)) : ratio.cut(ImageIO.read(new ByteArrayInputStream(bytes)));
        if (!jpeg) {
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage target = new BufferedImage(w, h, jpeg ? image.getType() : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            // 因为有的图片背景是透明色，所以用白色填充 FIXED
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1));
            g.fillRect(0, 0, w, h);
            g.drawImage(image, 0, 0, w, h, null); //image.getScaledInstance(w, h, Image.SCALE_SMOOTH)
            g.dispose();
            image = target;
        }
        return storeMultiJPGFile(dir, fileid, widths, ratio, image, null);
    }

    private static final int[] WIDTHS_ONEL = new int[]{0};

    public final String storeMultiJPGFile(final String dir, final String fileid0, int[] widths, final ImageRatio ratio, BufferedImage srcImage, Runnable runner) throws IOException {
        if (widths == null) widths = WIDTHS_ONEL;
        final File[] facefiles = new File[widths.length];
        srcImage = ratio == null ? srcImage : ratio.cut(srcImage);
        final String fileid = (fileid0 == null || fileid0.isEmpty()) ? randomFileid() : fileid0;
        for (int i = 0; i < widths.length; i++) {
            facefiles[i] = createFile((widths.length > 1 && widths[i] > 0) ? (dir + "_" + widths[i]) : dir, fileid, "jpg_tmp");
            BufferedImage target;
            if (widths[i] > 0) {
                int with = widths[i];
                int height = ratio == null ? (srcImage.getHeight() * with / srcImage.getWidth()) : ratio.height(with);
                target = new BufferedImage(with, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1));
                g.fillRect(0, 0, with, height);
                g.drawImage(srcImage, 0, 0, with, height, null); //srcImage.getScaledInstance(with, height, Image.SCALE_SMOOTH)
                g.dispose();
            } else {
                target = srcImage;
            }
            if (facefiles[i].getParentFile().isFile()) facefiles[i].getParentFile().delete();
            facefiles[i].getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(facefiles[i]);
//            com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(out);
//            com.sun.image.codec.jpeg.JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(target);
//            encoder.setJPEGEncodeParam(param);
//            encoder.encode(target);
            ImageIO.write(target, "jpeg", out);
            out.close();
        }
        String rs = "";
        for (int i = 0; i < facefiles.length; i++) {
            File newfile = new File(facefiles[i].getPath().replace("_tmp", ""));
            Files.move(facefiles[i].toPath(), newfile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            facefiles[i] = newfile;
            rs = newfile.getName();
        }
        if (runner != null) runner.run();
        return rs;
    }

    final File storeFile(String dir, String filename, String extension, String content) throws IOException {
        return storeFile(dir, filename, extension, Long.MAX_VALUE, new ByteArrayInputStream(content.getBytes("UTF-8")));
    }

    public final File storeFile(String dir, String filename, String extension, long max, InputStream in) throws IOException {
        final File file = createFile(dir, filename, extension);
        final byte[] bytes = new byte[4096];
        int pos;
        File temp = new File(file.getPath() + ".temp");
        OutputStream out = new FileOutputStream(temp);
        while ((pos = in.read(bytes)) != -1) {
            if (max < 0) {
                out.close();
                temp.delete();
                file.delete();
                return null;
            }
            out.write(bytes, 0, pos);
            max -= pos;
        }
        out.close();
        Files.move(temp.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
        return file;
    }

    public final boolean renameTo(String dir, int[] widths, String oldfileid, String newfileid) {
        if (oldfileid == null || oldfileid.isEmpty()) return false;
        if (newfileid == null || newfileid.isEmpty()) return false;
        File oldfile = new File(files, dir + "/" + hashPath(oldfileid));
        if (oldfile.isFile()) {
            File newfile = new File(files, dir + "/" + hashPath(newfileid));
            newfile.getParentFile().mkdirs();
            oldfile.renameTo(newfile);
        }
        if (widths == null) return true;
        for (int i = 0; i < widths.length; i++) {
            oldfile = new File(files, dir + "_" + widths[i] + "/" + hashPath(oldfileid));
            if (!oldfile.isFile()) continue;
            File newfile = new File(files, dir + "_" + widths[i] + "/" + hashPath(newfileid));
            newfile.getParentFile().mkdirs();
            oldfile.renameTo(newfile);
        }
        return true;
    }

    /**
     * 创建新的文件名， filename =null 则会随机生成一个文件名
     * <p>
     * @param dir       files下的根目录
     * @param filename  不带后缀的文件名
     * @param extension 文件名后缀
     *
     * @return
     */
    final File createFile(String dir, String filename, String extension) {
        filename = filename == null ? randomFileid() : filename;
        String f = filename + "." + (extension.substring(extension.lastIndexOf('.') + 1)).toLowerCase();
        if (f.indexOf('/') <= 0) f = hashPath(f);
        if (files == null) initPath();
        File file = new File(files, dir + "/" + f);
        if (file.getParentFile().isFile()) file.getParentFile().delete();
        file.getParentFile().mkdirs();
        return file;
    }

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
     * @return 长度为26的随机字符串
     */
    private static String randomFileid() {
        byte[] bytes = new byte[16];
        numberGenerator.nextBytes(bytes);
        String s = new BigInteger(1, bytes).toString(32);
        if (s.charAt(0) == '-') s = s.substring(1);
        if (s.length() >= 26) return s;
        StringBuilder sb = new StringBuilder(26);
        for (int i = s.length(); i < 26; i++) sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    //根据文件名生产文件存放的子目录， 如: aabbccddee.png 的存放目录为 aa/bb/cc/dd/aabbccee.png
    public static String hashPath(String filename) {
        int pos = filename.indexOf('.') - 1;
        if (pos < 1) pos = filename.length() - 1;
        StringBuilder sb = new StringBuilder();
        pos = pos - (pos & 1);
        for (int i = 0; i < pos; i += 2) {
            sb.append(filename.charAt(i)).append(filename.charAt(i + 1)).append('/');
        }
        sb.append(filename);
        return sb.toString();
    }

    private static final SecureRandom numberGenerator = new SecureRandom();
}
