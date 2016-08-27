/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.awt.image.BufferedImage;
import org.redkale.convert.*;
import org.redkale.convert.json.JsonConvert;
import org.redkale.util.*;

/**
 *
 * @author zhangjx
 */
public final class ImageRatio {

    public static final ImageRatio RATIO_1_1 = new ImageRatio(1, 1);

    public static final ImageRatio RATIO_3_2 = new ImageRatio(3, 2);

    public static final ImageRatio RATIO_4_3 = new ImageRatio(4, 3);

    public static final ImageRatio RATIO_8_5 = new ImageRatio(8, 5);

    public static final ImageRatio RATIO_16_9 = new ImageRatio(16, 9);

    public static final ImageRatio RATIO_16_10 = new ImageRatio(16, 10);

    private final int width;

    private final int height;

    private ImageRatio(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static ImageRatio create(int width, int height) {
        if (width < 1 || height < 1) return null;
        if (width == RATIO_1_1.width && height == RATIO_1_1.height) return RATIO_1_1;
        if (width == RATIO_3_2.width && height == RATIO_3_2.height) return RATIO_3_2;
        if (width == RATIO_4_3.width && height == RATIO_4_3.height) return RATIO_4_3;
        if (width == RATIO_8_5.width && height == RATIO_8_5.height) return RATIO_8_5;
        if (width == RATIO_16_9.width && height == RATIO_16_9.height) return RATIO_16_9;
        if (width == RATIO_16_10.width && height == RATIO_16_10.height) return RATIO_16_10;
        return new ImageRatio(width, height);
    }

    //根据高计算等比的宽度
    public int width(int h) {
        return h * this.width / this.height;
    }

    //根据宽计算等比的高度
    public int height(int w) {
        return w * height / this.width;
    }

    //判断图片是否与当前比例一致，返回false表示不同
    public boolean check(final BufferedImage image) {
        if (image == null) return false;
        return image.getWidth() * this.height == image.getHeight() * this.width;
    }

    //将图片按比例从中间裁剪
    public BufferedImage cut(final BufferedImage image) {
        final int oheight = image.getHeight();
        final int nheight = height(image.getWidth());
        if (oheight > nheight) { //太高了
            return image.getSubimage(0, 0, image.getWidth(), nheight);
        } else if (oheight < nheight) { //太宽了
            int nwidth = width(image.getHeight());
            return image.getSubimage((image.getWidth() - nwidth) / 2, 0, nwidth, image.getHeight());
        }
        return image;
    }

    @Override
    public String toString() {
        return JsonConvert.root().convertTo(this);
    }

    private static Creator<ImageRatio> creator() {
        return new Creator<ImageRatio>() {
            @Override
            @Creator.ConstructorParameters({"width", "height"})
            public ImageRatio create(Object... params) {
                return new ImageRatio((params[0] == null ? 0 : (Integer) params[0]), (params[1] == null ? 0 : (Integer) params[1]));
            }
        };
    }

    private static SimpledCoder<Reader, Writer, ImageRatio> createConvertCoder(final org.redkale.convert.ConvertFactory factory) {
        return new SimpledCoder<Reader, Writer, ImageRatio>() {

            //必须与EnMember[] 顺序一致
            private final DeMember[] deMembers = new DeMember[]{
                DeMember.create(factory, ImageRatio.class, "width", int.class),
                DeMember.create(factory, ImageRatio.class, "height", int.class)
            };

            //必须与DeMember[] 顺序一致
            private final EnMember[] enMembers = new EnMember[]{
                EnMember.create(Attribute.create(ImageRatio.class, "width", int.class, (t) -> t == null ? 0 : t.width, null), factory, int.class),
                EnMember.create(Attribute.create(ImageRatio.class, "height", int.class, (t) -> t == null ? 0 : t.height, null), factory, int.class)
            };

            @Override
            public void convertTo(Writer out, ImageRatio value) {
                if (value == null) {
                    out.writeObjectNull(ImageRatio.class);
                    return;
                }
                out.writeObjectB(value);
                for (EnMember member : enMembers) {
                    out.writeObjectField(member, value);
                }
                out.writeObjectE(value);
            }

            @Override
            public ImageRatio convertFrom(Reader in) {
                if (in.readObjectB(ImageRatio.class) == null) return null;
                int index = 0;
                final Object[] params = new Object[deMembers.length];
                while (in.hasNext()) {
                    DeMember member = in.readFieldName(deMembers); //读取字段名
                    in.readBlank(); //读取字段名与字段值之间的间隔符，JSON则是跳过冒号:
                    if (member == null) {
                        in.skipValue(); //跳过不存在的字段的值, 一般不会发生
                    } else {
                        params[index++] = member.read(in);
                    }
                }
                in.readObjectE(ImageRatio.class);
                return ImageRatio.create(params[0] == null ? 0 : (Integer) params[0], params[1] == null ? 0 : (Integer) params[1]);
            }
        };
    }
}
