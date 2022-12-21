package org.redkale.demo.info;

import org.redkale.demo.base.BaseEntity;
import org.redkale.persistence.*;

/**
 *
 * @author zhangjx
 */
@Table(comment = "手机号码归属地信息")
public class MobileGroup extends BaseEntity {

    public static final int MOBNET_YIDONG = 2; //移动运营商

    public static final int MOBNET_LIANTONG = 4; //联通运营商

    public static final int MOBNET_DIANXIN = 8; //电信运营商

    @Id
    @Column(length = 16, comment = "号码前缀")
    private String mobprefix = "";

    @Column(length = 32, comment = "省份")
    private String province = "";

    @Column(length = 32, comment = "城市")
    private String city = "";

    @Column(comment = "运营商; 2:移动; 4:联通; 8:电信;")
    private int mobnet;

    @Column(length = 32, comment = "区号")
    private String areacode = "";

    @Column(length = 32, comment = "邮政编码")
    private String postcode = "";

    public void setMobprefix(String mobprefix) {
        this.mobprefix = mobprefix;
    }

    public String getMobprefix() {
        return this.mobprefix;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getProvince() {
        return this.province;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return this.city;
    }

    public void setMobnet(int mobnet) {
        this.mobnet = mobnet;
    }

    public int getMobnet() {
        return this.mobnet;
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }

    public String getAreacode() {
        return this.areacode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getPostcode() {
        return this.postcode;
    }
}
