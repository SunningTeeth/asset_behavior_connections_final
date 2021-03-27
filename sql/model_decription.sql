/*
Navicat MySQL Data Transfer

Source Server         : 192.168.9.184
Source Server Version : 50720
Source Host           : 192.168.9.184:3306
Source Database       : csp

Target Server Type    : MYSQL
Target Server Version : 50720
File Encoding         : 65001

Date: 2021-03-02 16:17:05
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for model_decription
-- ----------------------------
DROP TABLE IF EXISTS `model_decription`;
CREATE TABLE `model_decription` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `model_type` int(11) DEFAULT NULL COMMENT '模型分类：1 资产行为模型 ；2 扫描行为 ；3 Ddos ; 4 加密流量',
  `model_child_type` int(11) DEFAULT NULL COMMENT '模型子类类型：如果无子类为 0 ；有子类型的资产行为模型： 1 资产会话连接流量模型 ；2 资产会话连接频率模型 ； 3 资产行为连接关系模型 ； 4 资产行为应用访问流量模型 ； 5 资产行为应用访问频率模型； 6 资产行为应用访问地理位置模型',
  `model_name` varchar(1024) DEFAULT NULL COMMENT '模型名字',
  `model_function` text COMMENT '模型功能介绍',
  `model_runinfo` text COMMENT '模型运行时占用信息描述。目前采用内置。JSON格式。后面可以支持实时信息展示',
  `model_rate_timeunit_preset` varchar(50) DEFAULT NULL COMMENT '模型预置支持的建模或检测频率单位时间 采用 ‘;’ 隔开。例： mm;hh;dd',
  `model_result_span_preset` varchar(50) DEFAULT NULL COMMENT '模型结果周期预置支持的项。采用 ‘;’ 隔开 。例 1;2',
  `model_confidence_interval_enable` varchar(100) DEFAULT NULL COMMENT '模型是否支持置信度参数。0 不支持 。支持只有几个可选项 0.99;0.95;0.9;',
  `model_statistics_realtime` int(50) DEFAULT NULL COMMENT '统计模型 0 或者实时分析模型 1 。',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of model_decription
-- ----------------------------
INSERT INTO `model_decription` VALUES ('1', '1', '1', '资产会话连接流量模型', '建立资产单位时间内不同协议会话上下行流量大小模型，检测流量访问异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'hh;dd', '1;2', '0.99;0.95;0.90', '0');
INSERT INTO `model_decription` VALUES ('2', '1', '2', '资产会话连接频率模型', '建立资产单位时间内不同协议会话访问频次模型，检测访问频率异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'mm;hh;dd', '1;2;3', '0.90;0.95;0.99', '0');
INSERT INTO `model_decription` VALUES ('3', '1', '3', '资产行为-连接关系模型', '建立资产访问目的地址的IP网段模型，检测连接关系异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'hh;dd', '1;2', '0', '0');
INSERT INTO `model_decription` VALUES ('4', '1', '4', '资产行为应用访问流量模型', '建立资产单位时间内访问应用流量大小模型，检测访问应用流量异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'mm;hh;dd', '1;2', '0.90;0.95;0.99', '0');
INSERT INTO `model_decription` VALUES ('5', '1', '5', '资产行为应用访问频率模型', '建立资产单位时间内访问应用次数模型，检测访问应用频率异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'mm;hh;dd', '1;2', '0.90;0.95;0.99', '0');
INSERT INTO `model_decription` VALUES ('6', '1', '6', '资产行为应用访问地理位置模型', '建立资产单位时间内访问应用地理位置分布模型，检测访问应用地理位置异常。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'hh;dd', '1;2', '0', '0');
INSERT INTO `model_decription` VALUES ('7', '2', '0', '扫描行为模型', '检测资产是否被扫描。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'ss;mm;hh', '1;2', '0', '1');
INSERT INTO `model_decription` VALUES ('8', '3', '0', 'DDOS检测模型模型', '检测DDOS攻击。', '{\"info\":\"模型预计将占用系统部分资源\"}', 'ss;mm;hh', '1;2', '0', '1');
INSERT INTO `model_decription` VALUES ('9', '4', '0', '加密流量检测模型', '加密流量异常检测', '{\"info\":\"模型预计将占用系统部分资源\"}', 'ss;mm;hh', '1;2', '0', '1');
