package org.redkale.demo.file;

import org.redkale.source.FilterBean;

public class FileCutBean implements FilterBean{

	private String filename;
	
	private int x1;
	
	private int x2;
	
	private int y1;
	
	private int y2;
	
	private int w;
	
	private int h;
	
	private int ow;
	
	private int oh;
	
	

	public int getOw() {
		return ow;
	}

	public void setOw(int ow) {
		this.ow = ow;
	}

	public int getOh() {
		return oh;
	}

	public void setOh(int oh) {
		this.oh = oh;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getX1() {
		return x1;
	}

	public void setX1(int x1) {
		this.x1 = x1;
	}

	public int getX2() {
		return x2;
	}

	public void setX2(int x2) {
		this.x2 = x2;
	}

	public int getY1() {
		return y1;
	}

	public void setY1(int y1) {
		this.y1 = y1;
	}

	public int getY2() {
		return y2;
	}

	public void setY2(int y2) {
		this.y2 = y2;
	}

	public int getW() {
		return w;
	}

	public void setW(int w) {
		this.w = w;
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}
	
	
}
