/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.imageio.*;
import org.redkale.annotation.Resource;
import org.redkale.demo.base.BaseService;
import org.redkale.util.*;

/**
 * 文件
 *
 * @author zhangjx
 */
public class FileService extends BaseService {

    static final int[] goods_widths = {800};

    //files 目录
    private File files;

    //自定义的文件根目录，没有设置默认使用 {home}/files 值
    @Resource(name = "files.root", required = false)
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

    static final int[] face_widths = {256}; //头像有三种规格: 64*64、256 512*512

    public final String storeFace(int userid, BufferedImage srcImage) throws IOException {
        return storeMultiJPGFile("face", Integer.toString(userid, 36), face_widths, ImageRatio.RATIO_1_1, srcImage, null);
    }

    public static java.util.List<File> list(File file, FilenameFilter filter) {
        return list(file, new ArrayList<>(), filter);
    }

    public static java.util.List<File> list(File file, java.util.List<File> list) {
        return list(file, list, null);
    }

    private static java.util.List<File> list(File file, java.util.List<File> list, FilenameFilter filter) {
        if (file == null) return list;
        if (file.isFile()) {
            if (filter == null || filter.accept(file, file.getPath())) list.add(file);
        } else if (file.isDirectory()) {
            for (File f : file.listFiles(filter)) {
                list(f, list, filter);
            }
        }
        return list;
    }

    /**
     * （1）JPEG
     * - 文件头标识 (2 bytes): 0xff, 0xd8 (SOI) (JPEG 文件标识)
     * - 文件结束标识 (2 bytes): 0xff, 0xd9 (EOI)
     * （2）TGA
     * - 未压缩的前5字节 00 00 02 00 00
     * - RLE压缩的前5字节 00 00 10 00 00
     * （3）PNG
     * - 文件头标识 (8 bytes) 89 50 4E 47 0D 0A 1A 0A
     * （4）GIF
     * - 文件头标识 (6 bytes) 47 49 46 38 39(37) 61，字符即： G I F 8 9 (7) a
     * （5）BMP
     * - 文件头标识 (2 bytes) 42 4D，字符即： B M
     * （6）TIFF
     * - 文件头标识 (2 bytes) 4D 4D 或 49 49
     * （7）ICO
     * - 文件头标识 (8 bytes) 00 00 01 00 01 00 20 20
     * （8）CUR
     * - 文件头标识 (8 bytes) 00 00 02 00 01 00 20 20
     *
     * @param dir
     * @param fileid0
     * @param filename
     * @param widths
     * @param ratio
     * @param bytes
     * @param runner
     *
     * @return
     * @throws IOException
     */
    public final String storeMultiJPGFile(final String dir, final String fileid0, final String filename, int[] widths, final ImageRatio ratio, byte[] bytes, Runnable runner) throws IOException {
        if (bytes == null) return "";
        final boolean webp = (bytes[0] & 0xFF) == 'W' && (bytes[1] & 0xFF) == 'E' && (bytes[2] & 0xFF) == 'B' && (bytes[3] & 0xFF) == 'P'; //WEBP
        final boolean riff = (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46; //RIFF
        final boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[bytes.length - 2] & 0xFF) == 0xFF && (bytes[bytes.length - 1] & 0xFF) == 0xD9;
        final boolean png = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
        final boolean gif = (bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49 && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38 && ((bytes[4] & 0xFF) == 0x39 || (bytes[4] & 0xFF) == 0x37) && (bytes[5] & 0xFF) == 0x61;
        String postfix = "face".equals(dir) ? ".jpg" : null;
        if (ratio == null) {
            if (png) {
                final String fileid = (fileid0 == null || fileid0.isEmpty()) ? randomFileid() : fileid0;
                if (postfix == null) {
                    postfix = filename.substring(filename.lastIndexOf('.') + 1);
                    postfix = "." + (postfix.isEmpty() || "jpg".equalsIgnoreCase(postfix) || "jpeg".equalsIgnoreCase(postfix) ? "png" : postfix);
                }
                final File file = createFile(dir, fileid, postfix);
                BufferedImage srcImage = ImageIO.read(new ByteArrayInputStream(bytes));
                int width = srcImage.getWidth();
                int height = srcImage.getHeight();
                BufferedImage newBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
                Graphics2D graphics = newBufferedImage.createGraphics();
                graphics.setBackground(new Color(255, 255, 255));
                graphics.clearRect(0, 0, width, height);
                graphics.drawImage(srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
                ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("png").next();
                FileOutputStream out = new FileOutputStream(file);
                imageWriter.setOutput(ImageIO.createImageOutputStream(out));
                imageWriter.write(new IIOImage(newBufferedImage, null, null));
                out.flush();
                //ImageIO.write(bufferedImage, "png", file);
                if (finest) logger.log(Level.FINEST, "png有损压缩: 原大小:" + bytes.length + ", 新大小: " + file.length() + ", 压缩率:" + (file.length() * 10000 / bytes.length) / 100 + "%");
                return fileid + postfix;
            } else if (jpeg) {
                final String fileid = (fileid0 == null || fileid0.isEmpty()) ? randomFileid() : fileid0;
                if (postfix == null) {
                    postfix = filename.substring(filename.lastIndexOf('.') + 1);
                    postfix = "." + (postfix.isEmpty() || "png".equalsIgnoreCase(postfix) ? "jpg" : postfix);
                }
                final File file = createFile(dir, fileid, postfix);
                //https://github.com/coobird/thumbnailator
                ImageWriteParam imgWriteParams = new javax.imageio.plugins.jpeg.JPEGImageWriteParam(null);
                // 要使用压缩，必须指定压缩方式为MODE_EXPLICIT
                imgWriteParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // 这里指定压缩的程度，参数quality是取值0~1范围内，
                imgWriteParams.setCompressionQuality(Math.min(1023 * 1024f / bytes.length, 0.5f));
                imgWriteParams.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                // 使用RGB格式
                ColorModel colorModel = ColorModel.getRGBdefault();
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
                // 指定压缩时使用的色彩模式
                imgWriteParams.setDestinationType(new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(bufferedImage.getWidth(), bufferedImage.getHeight())));
                // 指定写图片的方式为 png
                ImageWriter imgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                imgWriter.reset();
                FileOutputStream out = new FileOutputStream(file);
                // 必须先指定out值，才能调用write方法, ImageOutputStream可以通过任何OutputStream构造
                imgWriter.setOutput(ImageIO.createImageOutputStream(out));
                // 调用write方法，就可以向输入流写图片
                imgWriter.write(null, new IIOImage(bufferedImage, null, null), imgWriteParams);
                bufferedImage.flush();
                out.flush();
                //ImageIO.write(bufferedImage, "png", file);
                if (finest) logger.log(Level.FINEST, "jpg有损压缩: 原大小:" + bytes.length + ", 新大小: " + file.length() + ", 压缩率:" + (file.length() * 10000 / bytes.length) / 100 + "%");
                return fileid + postfix;
            } else if (gif) {
                final String fileid = (fileid0 == null || fileid0.isEmpty()) ? randomFileid() : fileid0;
                if (postfix == null) postfix = ".gif";
                final File file = createFile(dir, fileid, postfix);
                FileOutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.flush();
                if (finest) logger.log(Level.FINEST, "gif无需压缩: 原大小:" + bytes.length + ", 新大小: " + file.length() + ", 压缩率:" + (file.length() * 10000 / bytes.length) / 100 + "%");
                return fileid + postfix;
            } else if (riff || webp) {
                final String fileid = (fileid0 == null || fileid0.isEmpty()) ? randomFileid() : fileid0;
                if (postfix == null) postfix = ".webp";
                final File file = createFile(dir, fileid, postfix);
                FileOutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.flush();
                if (finest) logger.log(Level.FINEST, "webp无需压缩: 原大小:" + bytes.length + ", 新大小: " + file.length() + ", 压缩率:" + (file.length() * 10000 / bytes.length) / 100 + "%");
                return fileid + postfix;
            } else {
                if (!jpeg && !png) logger.log(Level.WARNING, "不支持的图片格式, 头信息: " + Utility.joiningHex(bytes, 0, 8, ',') + ", filename=" + filename);
                return null;
            }
        } else {
            BufferedImage image = ratio.cut(ImageIO.read(new ByteArrayInputStream(bytes)));
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
            return storeMultiJPGFile(dir, fileid0, widths, ratio, image, null);
        }
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
            if (widths[i] >= 0) {
                int with = widths[i] == 0 ? srcImage.getWidth() : widths[i];
                int height = widths[i] == 0 ? srcImage.getHeight() : (ratio == null ? (srcImage.getHeight() * with / srcImage.getWidth()) : ratio.height(with));
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
        File file = createFile(dir, filename, extension);
        final byte[] bytes = new byte[4096];
        int pos;
        File temp = new File(file.getPath() + ".temp");
        try (OutputStream out = new FileOutputStream(temp)) {
            while ((pos = in.read(bytes)) != -1) {
                if (file == null) continue;
                if (max < 0) {
                    out.close();
                    temp.delete();
                    file.delete();
                    file = null;
                    continue;
                    //return null;
                }
                out.write(bytes, 0, pos);
                max -= pos;
            }
        }
        if (file == null) return file;
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
     * @param dir               files下的根目录
     * @param fileNameNoPostfix 不带后缀的文件名
     * @param extension         文件名后缀
     *
     * @return
     */
    final File createFile(String dir, String fileNameNoPostfix, String extension) {
        fileNameNoPostfix = fileNameNoPostfix == null ? randomFileid() : fileNameNoPostfix;
        String f = fileNameNoPostfix + "." + (extension.substring(extension.lastIndexOf('.') + 1)).toLowerCase();
        if (f.indexOf('/') <= 0) f = hashPath(f);
        if (files == null) initPath();
        File file = new File(files, dir + "/" + f);
        if (file.getParentFile().isFile()) file.getParentFile().delete();
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * <pre>
     *     int   10万-100万     (36进制 4位)  255t - lflr         长度4     直接访问: rewrite "^/dir/(\w+)/(\w{4})$" /$1/$2.jpg break;
     *     int  1000万-6000万   (36进制 5位)  5yc1t - zq0an
     *     int    2亿-20亿      (36进制 6位)  3b2ozl - x2qxvk
     *    long   30亿-770亿     (36进制 7位)  1dm4etd - zdft88v
     *    long  1000亿-2万亿    (36进制 8位)  19xtf1tt - piscd0jj 长度5-8   rewrite "^/dir/(\w+)/((\w{4})(\w+))$" /$1/$3/$2.jpg break;
     *    随机文件名:   (十六进制 16位)
     *      #文件名 长度: 32    nginx不支持$10、$11
     *      rewrite "^/dir/(\\w+)/((\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})\\..*)$" /$1/$3/$4/$5/$6/$7/$8/$9/$2 break;
     *
     * </pre>
     *
     * ningx配置: （顺序不能改）
     *
     * <pre>
     * location ~ ^/dir/face/.*$ {
     *      root /usr/local/redsns-server/files;
     *      rewrite "^/dir/(\w+)/(\w{4})$" /$1/$2.jpg last;
     *      rewrite "^/dir/(\w+)/((\w{4})(\w+))$" /$1/$3/$2.jpg last;
     * }
     *
     * location ~ ^/dir/.*$ {
     *      root /usr/local/redsns-server/files;
     *      rewrite "^/dir/(\\w+)/((\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})(\\w{4})\\..*)$" /$1/$3/$4/$5/$6/$7/$8/$9/$2 break;
     * }
     *
     * location ~ ^/face/.*$ {
     *      root /usr/local/redsns-server/files;
     *      error_page 404 =200 /face/def.jpg;
     * }
     * </pre>
     *
     *
     * @return 长度为32的随机字符串
     */
    private static String randomFileid() {
        return Utility.binToHexString(Utility.generateRandomBytes(16));
    }

    //根据文件名生产文件存放的子目录， 如: aabbccddee.png 的存放目录为 aabb/ccdd/aabbccee.png
    public static String hashPath(String fileName) {
        int p = fileName.indexOf('.');
        int pos = p > 0 ? p : fileName.length();
        StringBuilder sb = new StringBuilder();
        pos = (pos - 1) / 4 * 4;
        for (int i = 0; i < pos; i += 4) {
            sb.append(fileName.charAt(i)).append(fileName.charAt(i + 1)).append(fileName.charAt(i + 2)).append(fileName.charAt(i + 3)).append('/');
        }
        sb.append(fileName);
        return sb.toString();
    }

}
