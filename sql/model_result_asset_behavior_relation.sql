/*
Navicat MySQL Data Transfer

Source Server         : 192.168.3.168
Source Server Version : 50720
Source Host           : 192.168.3.168:3306
Source Database       : csp

Target Server Type    : MYSQL
Target Server Version : 50720
File Encoding         : 65001

Date: 2021-03-27 13:14:54
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for model_result_asset_behavior_relation
-- ----------------------------
DROP TABLE IF EXISTS `model_result_asset_behavior_relation`;
CREATE TABLE `model_result_asset_behavior_relation` (
  `id` varchar(32) NOT NULL COMMENT 'ID',
  `modeling_params_id` varchar(32) NOT NULL COMMENT '模型参数ID。根据模型参数ID可以知道建模的类型及子类型',
  `src_id` varchar(100) NOT NULL COMMENT '源资产ID',
  `src_ip` text COMMENT '资产源IP',
  `dst_ip_segment` text COMMENT '目的IP网段。json格式，key 从1开始到 （模型结果时长/频率）例，一周每天的访问模型 结果为\r\n[    {"name": "1",\r\n   "value": "IP1,IP2"},\r\n    {"name": "2",\r\n   "value": "IP1,IP2"},\r\n    {"name": "3",\r\n   "value": "IP1,IP2"},\r\n    {"name": "4",\r\n   "value": "IP1,IP2"},\r\n    {"name": "5",\r\n   "value": "IP1,IP2"}\r\n]\r\n。\r\n\r\nvalue多个时候用 英文逗号 '','' 隔开',
  `time` datetime DEFAULT NULL COMMENT '数据插入时间',
  PRIMARY KEY (`modeling_params_id`,`src_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
