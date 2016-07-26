/*
SQLyog Ultimate v11.24 (32 bit)
MySQL - 5.7.11-log : Database - red_demo
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`red_demo` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `red_demo`;

/*Table structure for table `randomcode` */

DROP TABLE IF EXISTS `randomcode`;

CREATE TABLE `randomcode` (
  `randomcode` varchar(128) NOT NULL DEFAULT '' COMMENT '[验证码]',
  `userid` int(11) NOT NULL DEFAULT '0' COMMENT '[所属用户ID]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[类型]: 10:手机号码注册;20:短信重置密码;30:修改手机号码; ;60:邮件重置密码;70:更改邮箱绑定;',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  PRIMARY KEY (`randomcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `randomcode` */


/*Table structure for table `randomcodehis` */

DROP TABLE IF EXISTS `randomcodehis`;

CREATE TABLE `randomcodehis` (
  `seqid` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增长序号',
  `randomcode` varchar(128) NOT NULL DEFAULT '' COMMENT '[验证码]',
  `userid` int(11) NOT NULL DEFAULT '0' COMMENT '[所属用户ID]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[类型]: 10:手机号码注册;20:短信重置密码;30:修改手机号码; ;60:邮件重置密码;70:更改邮箱绑定;',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  `retcode` int(11) NOT NULL DEFAULT '0' COMMENT '[结果]: 2: 过期; 4已处理;',
  `updatetime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[更新时间]',
  PRIMARY KEY (`seqid`)
) ENGINE=InnoDB AUTO_INCREMENT=100000001 DEFAULT CHARSET=utf8;

/*Data for the table `randomcodehis` */


DROP TABLE IF EXISTS `smsrecord`;

CREATE TABLE `smsrecord` (
  `smsid` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '短信ID',
  `smstype` SMALLINT(6) NOT NULL DEFAULT '0' COMMENT '短信类型; 10:手机注册；20:重置密码；30:修改手机；40:登录；',
  `status` SMALLINT(6) NOT NULL DEFAULT '10' COMMENT '状态; 10:未发送; 20:已发送; 30:发送失败;',
  `mobile` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '手机号码',
  `content` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '短信内容',
  `resultdesc` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '返回结果',
  `createtime` BIGINT(20) NOT NULL DEFAULT '0' COMMENT '生成时间，单位毫秒',
  PRIMARY KEY (`smsid`)
) ENGINE=INNODB AUTO_INCREMENT=200000001 DEFAULT CHARSET=utf8 COMMENT='短信发送记录表';


DROP TABLE IF EXISTS `noticerecord`;

CREATE TABLE `noticerecord` (
  `noticeid` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `userid` int(11) NOT NULL DEFAULT '0' COMMENT '用户ID',
  `status` smallint(6) NOT NULL DEFAULT '0' COMMENT '状态; 20:未读;60:已读;70:过期',
  `apptoken` varchar(128) NOT NULL DEFAULT '' COMMENT '设备推送ID',
  `content` varchar(4096) NOT NULL DEFAULT '' COMMENT '消息体',
  `resultdesc` varchar(4096) NOT NULL DEFAULT '' COMMENT '消息体',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  PRIMARY KEY (`noticeid`)
) ENGINE=InnoDB AUTO_INCREMENT=3000000001 DEFAULT CHARSET=utf8 COMMENT='消息推送表';

/*Table structure for table `userdetail` */

DROP TABLE IF EXISTS `userdetail`;

CREATE TABLE `userdetail` (
  `userid` int(11) NOT NULL AUTO_INCREMENT COMMENT '[用户ID]',
  `account` varchar(128) NOT NULL DEFAULT '' COMMENT '[用户账号]',
  `username` varchar(128) NOT NULL DEFAULT '' COMMENT '[用户昵称]',
  `type` smallint(5) NOT NULL DEFAULT '0' COMMENT '[用户类型]',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
  `mobile` varchar(128) NOT NULL DEFAULT '' COMMENT '[手机号码]',
  `email` varchar(128) NOT NULL DEFAULT '' COMMENT '[邮箱地址]',
  `wxunionid` varchar(255) NOT NULL DEFAULT '' COMMENT '微信openid',
  `qqopenid` varchar(255) NOT NULL DEFAULT '' COMMENT 'QQ openid]',
  `apptoken` varchar(255) NOT NULL DEFAULT '' COMMENT 'APP的设备ID',
  `status` smallint(5) NOT NULL DEFAULT '0' COMMENT '[状态]: 10:正常;20:待审批;40:冻结;50:隐藏;60:关闭;70:过期;80:删除;',
  `infotime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[资料更新时间]',
  `gender` smallint(5) NOT NULL DEFAULT '0' COMMENT '[性别]：2：男； 4:女；',
  `updatetime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[更新时间]',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '[备注]',
  `regtype` smallint(6) NOT NULL DEFAULT '0' COMMENT '[注册类型]',
  `createtime` bigint(20) NOT NULL DEFAULT '0' COMMENT '[创建时间]',
  `regagent` varchar(255) NOT NULL DEFAULT '' COMMENT '[注册终端]',
  `regaddr` varchar(64) NOT NULL DEFAULT '' COMMENT '[注册IP]',
  PRIMARY KEY (`userid`)
) ENGINE=InnoDB AUTO_INCREMENT=200000001 DEFAULT CHARSET=utf8;

/*Data for the table `userdetail` */


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
