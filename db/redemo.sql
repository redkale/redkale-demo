
/*!40101 SET NAMES UTF8MB4 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`redemo_platf` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `redemo_platf`;

/*Table structure for table `payaction` */

DROP TABLE IF EXISTS `payaction`;

CREATE TABLE `payaction` (
  `payactid` varchar(64) NOT NULL DEFAULT '' COMMENT '记录ID 值=create36time(9位)+UUID(32位)',
  `payno` varchar(128) NOT NULL DEFAULT '' COMMENT '支付编号',
  `paytype` smallint(6) NOT NULL DEFAULT '10' COMMENT '//支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;',
  `acturl` varchar(1024) NOT NULL DEFAULT '' COMMENT '请求的URL',
  `requestjson` varchar(2048) NOT NULL DEFAULT '' COMMENT '支付接口请求对象',
  `responsetext` varchar(5120) NOT NULL DEFAULT '' COMMENT '支付接口返回的原始结果',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间，单位毫秒',
  PRIMARY KEY (`payactid`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='支付接口结果表';

/*Data for the table `payaction` */

/*Table structure for table `payrecord` */

DROP TABLE IF EXISTS `payrecord`;

CREATE TABLE `payrecord` (
  `payno` varchar(64) NOT NULL DEFAULT '' COMMENT '支付编号; 值=orderno+createtime36进制(9位)',
  `thirdpayno` varchar(128) NOT NULL DEFAULT '' COMMENT '第三方支付订单号',
  `appid` varchar(128) NOT NULL DEFAULT '' COMMENT '支付APP应用ID',
  `paytype` smallint(6) NOT NULL DEFAULT '10' COMMENT '//支付类型:  10: 信用/虚拟支付; 11:人工支付; 12:银联支付; 13:微信支付; 14:支付宝支付;15:易宝支付;',
  `payway` smallint(6) NOT NULL DEFAULT '10' COMMENT '支付渠道:  10: 信用/虚拟支付; 20:人工支付; 30:APP支付; 40:网页支付; 50:机器支付;',
  `userno` varchar(64) NOT NULL DEFAULT '' COMMENT '付款人用户信息',
  `paytitle` varchar(128) NOT NULL DEFAULT '' COMMENT '订单标题',
  `paybody` varchar(255) NOT NULL DEFAULT '' COMMENT '订单内容描述',
  `notifyurl` varchar(255) NOT NULL DEFAULT '' COMMENT '支付回调连接',
  `ordertype` smallint(6) NOT NULL DEFAULT '0' COMMENT '订单类型',
  `orderno` varchar(64) NOT NULL DEFAULT '' COMMENT '订单编号',
  `paystatus` smallint(6) NOT NULL DEFAULT '10' COMMENT '支付状态; 10:待支付; 30:已支付; 50:待退款; 70:已退款; 90:已关闭;',
  `payedmoney` bigint(20) NOT NULL DEFAULT '0' COMMENT '实际支付金额 单位人民币分；',
  `money` bigint(20) NOT NULL DEFAULT '0' COMMENT '订单金额，单位人民币分；',
  `requestjson` varchar(1024) NOT NULL DEFAULT '' COMMENT '支付接口请求对象',
  `responsetext` varchar(10240) NOT NULL DEFAULT '' COMMENT '支付接口返回的原始结果',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '支付开始时间，单位毫秒',
  `finishtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '支付结束时间，单位毫秒',
  `clienthost` varchar(64) NOT NULL DEFAULT '' COMMENT '客户端请求的HOST',
  `clientaddr` varchar(128) NOT NULL DEFAULT '' COMMENT '客户端生成时的IP',
  PRIMARY KEY (`payno`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='支付表';

/*Data for the table `payrecord` */

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;





/*!40101 SET NAMES UTF8MB4 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


/*Table structure for table `noticerecord` */

DROP TABLE IF EXISTS `noticerecord`;

CREATE TABLE `noticerecord` (
  `noticeid` varchar(64) NOT NULL DEFAULT '' COMMENT '消息ID 值=create36time(9位)+UUID(32位)',
  `userid` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',
  `status` smallint(6) NOT NULL DEFAULT '0' COMMENT '状态; 20:未读;60:已读;70:过期',
  `appos` varchar(16) NOT NULL DEFAULT '' COMMENT 'APP的设备系统(小写); android/ios',
  `apptoken` varchar(128) NOT NULL DEFAULT '' COMMENT '设备推送ID',
  `content` varchar(4096) NOT NULL DEFAULT '' COMMENT '消息体',
  `resultdesc` varchar(4096) NOT NULL DEFAULT '' COMMENT '消息体',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  PRIMARY KEY (`noticeid`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='消息推送表';

/*Data for the table `noticerecord` */

/*Table structure for table `randomcode` */

DROP TABLE IF EXISTS `randomcode`;

CREATE TABLE `randomcode` (
  `randomcode` varchar(128) NOT NULL DEFAULT '' COMMENT '手机-验证码数据对',
  `userid` bigint(20) NOT NULL DEFAULT '0' COMMENT '[所属用户ID]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[类型]: 10:手机号码注册;20:短信重置密码;30:修改手机号码;40:用户验证码登录;50:发送原手机号码;60:邮件重置密码;70:更改邮箱绑定;',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  PRIMARY KEY (`randomcode`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4;

/*Data for the table `randomcode` */

/*Table structure for table `randomcodehis` */

DROP TABLE IF EXISTS `randomcodehis`;

CREATE TABLE `randomcodehis` (
  `seqid` varchar(64) NOT NULL DEFAULT '' COMMENT '记录ID 值=create36time(9位)+UUID(32位)',
  `randomcode` varchar(128) NOT NULL DEFAULT '' COMMENT '[验证码]',
  `userid` bigint(20) NOT NULL DEFAULT '0' COMMENT '[所属用户ID]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[类型]: 10:手机号码注册;20:短信重置密码;30:修改手机号码; ;60:邮件重置密码;70:更改邮箱绑定;',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  `retcode` int(11) NOT NULL DEFAULT '0' COMMENT '[结果]: 2: 过期; 4已处理;',
  `updatetime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[更新时间]',
  PRIMARY KEY (`seqid`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4;

/*Data for the table `randomcodehis` */

/*Table structure for table `smsrecord` */

DROP TABLE IF EXISTS `smsrecord`;

CREATE TABLE `smsrecord` (
  `smsid` varchar(64) NOT NULL DEFAULT '' COMMENT '短信ID 值=create36time(9位)+UUID(32位)',
  `smstype` smallint(6) NOT NULL DEFAULT '0' COMMENT '短信类型; 10:验证码；20:营销短信；',
  `codetype` smallint(6) NOT NULL DEFAULT '0' COMMENT '验证码类型; 10:手机注册；20:重置密码；30:修改手机；40:登录；',
  `status` smallint(6) NOT NULL DEFAULT '10' COMMENT '状态; 10:未发送; 20:已发送; 30:发送失败;',
  `smscount` int(11) NOT NULL DEFAULT '0' COMMENT '群发的短信条数',
  `mobcount` int(11) NOT NULL DEFAULT '1' COMMENT '群发的手机号码数',
  `mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '手机号码',
  `mobiles` varchar(2048) NOT NULL DEFAULT '' COMMENT '群发的手机号码集合，多个用;隔开,最多100条',
  `content` varchar(1024) NOT NULL DEFAULT '' COMMENT '短信内容',
  `resultdesc` varchar(1024) NOT NULL DEFAULT '' COMMENT '返回结果',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '生成时间，单位毫秒',
  PRIMARY KEY (`smsid`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='短信发送记录表';

/*Data for the table `smsrecord` */

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;




/*!40101 SET NAMES UTF8MB4 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*Table structure for table `mobilegroup` */

DROP TABLE IF EXISTS `mobilegroup`;

CREATE TABLE `mobilegroup` (
  `mobprefix` varchar(16) NOT NULL DEFAULT '' COMMENT '号码前缀',
  `province` varchar(32) NOT NULL DEFAULT '' COMMENT '省份',
  `city` varchar(32) NOT NULL DEFAULT '' COMMENT '城市',
  `mobnet` int(11) NOT NULL DEFAULT '0' COMMENT '运营商; 2:移动; 4:联通; 8:电信;',
  `areacode` varchar(32) NOT NULL DEFAULT '' COMMENT '区号',
  `postcode` varchar(32) NOT NULL DEFAULT '' COMMENT '邮政编码',
  PRIMARY KEY (`mobprefix`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='手机号码归属地信息';

/*Data for the table `mobilegroup` */

/*Table structure for table `userdetail` */

DROP TABLE IF EXISTS `userdetail`;

CREATE TABLE `userdetail` (
  `userid` int(11) NOT NULL DEFAULT '0' COMMENT '[用户ID] 值从2_0000_0001开始; 36进制固定长度为6位',
  `account` varchar(128) NOT NULL DEFAULT '' COMMENT '[用户账号]',
  `username` varchar(128) NOT NULL DEFAULT '' COMMENT '[用户昵称]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[用户类型]',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
  `mobile` varchar(128) NOT NULL DEFAULT '' COMMENT '[手机号码]',
  `mobnet` int(11) NOT NULL DEFAULT '0' COMMENT '运营商; 2:移动; 4:联通; 8:电信;',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '[邮箱地址]',
  `wxunionid` varchar(255) NOT NULL DEFAULT '' COMMENT '微信openid',
  `qqopenid` varchar(255) NOT NULL DEFAULT '' COMMENT 'QQ openid',
  `appos` varchar(16) NOT NULL DEFAULT '' COMMENT 'APP的设备系统(小写); android/ios/web/wap',
  `apptoken` varchar(255) NOT NULL DEFAULT '' COMMENT 'APP的设备ID',
  `status` smallint(5) NOT NULL DEFAULT '0' COMMENT '[状态]: 10:正常;20:待审批;30:审批不通过;40:冻结;50:隐藏;60:关闭;70:过期;80:删除;',
  `gender` smallint(5) NOT NULL DEFAULT '0' COMMENT '[性别]：2：男； 4:女；',
  `updatetime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[更新时间]',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '[备注]',
  `regtype` smallint(6) NOT NULL DEFAULT '0' COMMENT '[注册类型]: 10:账号注册; 20:手机注册; 30:邮箱注册; 40:微信注册; 50:QQ注册',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  `regagent` varchar(255) NOT NULL DEFAULT '' COMMENT '[注册终端]',
  `regaddr` varchar(64) NOT NULL DEFAULT '' COMMENT '[注册IP]',
  PRIMARY KEY (`userid`),
  KEY `m` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8MB4 COMMENT='用户信息表';

/*Data for the table `userdetail` */

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
