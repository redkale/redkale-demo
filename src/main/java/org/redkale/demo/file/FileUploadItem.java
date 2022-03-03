/*
 */
package org.redkale.demo.file;

import org.redkale.demo.base.BaseBean;

/**
 *
 * @author zhangjx
 */
public class FileUploadItem extends BaseBean {

    private String fileid;

    private String fileName;

    private long fileLength;

    public FileUploadItem() {
    }

    public FileUploadItem(String fileid, String fileName, long fileLength) {
        this.fileid = fileid;
        this.fileName = fileName;
        this.fileLength = fileLength;
    }

    public String getFileid() {
        return fileid;
    }

    public void setFileid(String fileid) {
        this.fileid = fileid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

}
