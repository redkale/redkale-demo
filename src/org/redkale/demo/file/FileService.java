/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import com.sun.image.codec.jpeg.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import org.redkale.demo.base.BaseService;
import org.redkale.demo.user.UserService;
import org.redkale.service.RetResult;
import org.redkale.util.*;

/**
 * 文件同步流程： 同机房的进程的文件同步通过scp命令同步
 * < 不同机房的通过远程FileService调用， 只要另一机房进程有一个能成功同步过去，则结束同步， 如果全都同步不成功，则通过scp命令同步到对方文件下。
 * 不同机房的进程接收到同步命令后通过scp将文件同步到同机房下其他进程文件下。 > <暂未实现>
 *
 * @author zhangjx
 */
public class FileService extends BaseService {

    private static final String format = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%tL";

    private class SyncEntity {

        public final String command;

        public int trycount;

        public SyncEntity(String command) {
            this.command = command;
        }

        public void run(final boolean retry) throws IOException, InterruptedException {
            String[] ss = command.split("(?!'\\S+)\\s+(?![^']+')");
            List<String> cmds = new ArrayList<>(ss.length);
            for (String s : ss) {
                if (s.indexOf('\'') >= 0) {
                    cmds.add(s.replace('\'', ' '));
                } else {
                    cmds.addAll(Arrays.asList(s.split("\\s+")));
                }
            }
            long s = System.currentTimeMillis();
            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String result = Utility.read(proc.getInputStream()).replace("\n", " ");
            long e = System.currentTimeMillis() - s;
            if (finest) logger.finest("file async time : " + e + " ms; commonds: " + cmds.toString().replace(',', ' ') + "; result : " + result);
            if (!result.isEmpty()) {
                if (retry && (trycount++ > 3 || !syncQueue.offer(this))) fail(command);
            }
            if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                if (retry && (trycount++ > 3 || !syncQueue.offer(this))) fail(command);
            }
        }
    }

    private final BlockingQueue<SyncEntity> syncQueue = new ArrayBlockingQueue(1024);

    //files 目录
    private File files;

    private String homepath;

    private PrintStream syncstream;

    @Resource
    private UserService userService;

    @Resource(name = "APP_HOME")
    private File home;

    @Resource(name = "APP_ADDR")
    private String localAddress;

    @Resource(name = "APP_NODES")
    private HashMap<InetSocketAddress, String> appNodes;

    @Resource(name = "SNCP_GROUPS")
    private Set<String> groups;

    @Resource(name = "property.files.syncip")
    private String fileaddrs = "";

    @Resource(name = "property.files.syncmd")
    private String filescmd = "ssh root@{REMOTEIP} 'mkdir -p {FILEPATH}%2$s;scp -rp root@{LOCALIP}:{FILEPATH}%1$s {FILEPATH}%2$s'";

    private String[] groupSyncCmds;

    @Override
    public void init(AnyValue config) {
        initPath();
        if (System.getProperty("os.name").contains("Window")) return;
        if (this.filescmd == null || this.localAddress == null) return;
        if (this.appNodes == null || this.appNodes.isEmpty()) return;
        final String filessync = this.filescmd.replace("{FILEPATH}", homepath).replace("{LOCALIP}", this.localAddress);
        List<String> cmds = new ArrayList<>();
        if (filessync.contains("{REMOTEIP}")) {
            if (fileaddrs == null || fileaddrs.trim().isEmpty()) {
                if (appNodes != null && localAddress != null && groups != null) {
                    this.appNodes.forEach((k, v) -> {
                        if (groups.contains(v) && !localAddress.equals(k.getHostString())) {
                            cmds.add(filessync.replace("{REMOTEIP}", k.getHostString()));
                        }
                    });
                }
            } else {
                for (String ip : fileaddrs.split(";")) {
                    if (ip.trim().isEmpty()) continue;
                    cmds.add(filessync.replace("{REMOTEIP}", ip.trim()));
                }
            }
        } else {
            cmds.add(filessync);
        }
        if (cmds.isEmpty()) return;
        this.groupSyncCmds = cmds.toArray(new String[cmds.size()]);
        logger.finer("asynccmds = " + cmds);
        //启动文件同步任务队列
        new Thread() {
            {
                setName("Files-Sync-Thread");
                setDaemon(true);
            }

            @Override
            public void run() {
                while (true) {
                    String command = "";
                    try {
                        SyncEntity entity = syncQueue.take();
                        command = entity.command;
                        entity.run(true);
                    } catch (Exception e) {
                        fail(command);
                    }
                }
            }
        }.start();
    }

    //初始化静态资源的总文件目录
    private void initPath() {
        if (this.files != null) return;
        this.files = new File(home, "files");
        this.files.mkdirs();
        this.homepath = this.home.getPath();
        try {
            this.homepath = this.home.getCanonicalPath();
        } catch (Exception e) {
            logger.log(Level.WARNING, "path[" + files + "].getCanonicalPath error", e);
        }
    }

    @Override
    public void destroy(AnyValue config) {
        if (syncstream != null) syncstream.close();
    }

    //文件同步失败的异常处理，记录日志文件，可在此处添加异常消息上报处理
    private void fail(String command) {
        try {
            if (syncstream == null) {
                synchronized (this) {
                    if (syncstream == null) {
                        File syncfail = new File(home, "syncs");
                        syncfail.mkdirs();
                        syncstream = new PrintStream(new FileOutputStream(new File(syncfail, "sync_fail.txt"), true), true, "UTF-8");
                    }
                }
            }
            syncstream.print("/** " + String.format(format, System.currentTimeMillis()) + " */  " + command + "\r\n");
        } catch (IOException ex) {
            logger.log(Level.WARNING, this.getClass().getSimpleName() + " async file error (" + command + ")", ex);
        }
    }

    private static final int[] face_widths = {64, 128, 512}; //头像有三种规格: 64*64、128*128 512*512

    public final String storeFace(int userid, BufferedImage srcImage) throws IOException {
        return storeMultiJPGFile("face", Integer.toString(userid, 36), face_widths, ImageRatio.RATIO_1_1, srcImage, () -> userService.updateInfotime(userid));
    }

    public final String storeMultiJPGFile(final String dir, final String fileid, int[] widths, final ImageRatio ratio, byte[] bytes, Runnable runner) throws IOException {
        if (bytes == null) return "";
        final boolean jpeg = bytes[0] == 0xFF && bytes[1] == 0xD8 && bytes[bytes.length - 2] == 0xFF && bytes[bytes.length - 1] == 0xD9;
        BufferedImage image = ratio.cut(ImageIO.read(new ByteArrayInputStream(bytes)));
        if (!jpeg) {
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage target = new BufferedImage(w, h, jpeg ? image.getType() : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            // 因为有的图片背景是透明色，所以用白色填充 FIXED
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1));
            g.fillRect(0, 0, w, h);
            g.drawImage(image.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, w, h, null);
            g.dispose();
            image = target;
        }
        return storeMultiJPGFile(dir, null, widths, ratio, image, null);
    }

    public final String storeMultiJPGFile(final String dir, final String fileid, int[] widths, final ImageRatio ratio, BufferedImage srcImage, Runnable runner) throws IOException {
        final File[] facefiles = new File[widths.length];
        srcImage = ratio.cut(srcImage);
        for (int i = 0; i < widths.length; i++) {
            facefiles[i] = createFile(dir + "_" + widths[i], fileid, "jpg_tmp");
            int with = widths[i];
            int height = ratio.height(with);
            BufferedImage target = new BufferedImage(with, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1));
            g.fillRect(0, 0, with, height);
            g.drawImage(srcImage.getScaledInstance(with, height, Image.SCALE_SMOOTH), 0, 0, with, height, null);
            g.dispose();
            if (facefiles[i].getParentFile().isFile()) facefiles[i].getParentFile().delete();
            facefiles[i].getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(facefiles[i]);
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(target);
            encoder.setJPEGEncodeParam(param);
            encoder.encode(target);
            out.close();
        }
        for (int i = 0; i < facefiles.length; i++) {
            File newfile = new File(facefiles[i].getPath().replace("_tmp", ""));
            Files.move(facefiles[i].toPath(), newfile.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            facefiles[i] = newfile;
        }
        if (runner != null) runner.run();
        asyncFile(facefiles);
        return fileid + ".jpg";
    }

    final File storeFile(String dir, String filename, String extension, String content) throws IOException {
        return storeFile(true, dir, filename, extension, content);
    }

    final File storeFile(boolean sync, String dir, String filename, String extension, String content) throws IOException {
        return storeFile(sync, dir, filename, extension, Long.MAX_VALUE, new ByteArrayInputStream(content.getBytes("UTF-8")));
    }

    final File storeFile(String dir, String filename, String extension, long max, InputStream in) throws IOException {
        return storeFile(true, dir, filename, extension, max, in);
    }

    public final File storeFile(boolean sync, String dir, String filename, String extension, long max, InputStream in) throws IOException {
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
        if (sync) asyncFile(file);
        return file;
    }

    final void asyncFile(File... files) {
        if (groupSyncCmds == null) return;
        for (String synccmd : groupSyncCmds) {
            for (File file : files) {
                String path = file.getPath().substring(this.homepath.length());
                String command = String.format(synccmd, synccmd.contains("%1") ? path : path.substring(0, path.lastIndexOf('/')), path.substring(0, path.lastIndexOf('/')));
                syncQueue.add(new SyncEntity(command));
            }
        }
    }

    public RetResult syncFile(String path) {
        if (groupSyncCmds == null) return new RetResult(2010101);
        if (path.startsWith(this.homepath)) path = path.substring(this.homepath.length());
        for (String synccmd : groupSyncCmds) {
            String command = String.format(synccmd, synccmd.contains("%1") ? path : path.substring(0, path.lastIndexOf('/')), path.substring(0, path.lastIndexOf('/')));
            try {
                new SyncEntity(command).run(false);
            } catch (Exception e) {
                logger.log(Level.FINER, command + " sync file error", e);
                return new RetResult(2010201, command);
            }
        }
        return new RetResult();
    }

    /**
     * 创建新的文件名， filename =null 则会随机生成一个文件名
     * <p>
     * @param dir       files下的根目录
     * @param filename 不带后缀的文件名
     * @param extension 文件名后缀
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

    private static String randomFileid() {
        String s = Long.toString(Math.abs(System.nanoTime()), 32);
        if (s.length() < 12) {
            for (int i = s.length(); i < 12; i++) s = '0' + s;
        }
        return s;
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

}
