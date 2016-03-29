/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.file;

import java.awt.image.BufferedImage;

/**
 *
 * @author zhangjx
 */
public enum ImageRatio {
    RATIO_1_1(1, 1),
    RATIO_3_2(3, 2),
    RATIO_4_3(4, 3),
    RATIO_8_5(8, 5),
    RATIO_16_9(16, 9),
    RATIO_16_10(16, 10);

    private final int width;

    private final int height;

    private ImageRatio(int width, int height) {
        this.width = width;
        this.height = height;
    }

    //根据高计算等比的宽度
    public int width(int h) {
        return h * this.height / this.width;
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
}
