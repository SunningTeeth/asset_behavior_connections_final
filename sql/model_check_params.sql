/*
Navicat MySQL Data Transfer

Source Server         : 192.168.3.101
Source Server Version : 50720
Source Host           : 192.168.3.101:3306
Source Database       : csp

Target Server Type    : MYSQL
Target Server Version : 50720
File Encoding         : 65001

Date: 2021-03-27 13:25:50
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for model_check_params
-- ----------------------------
DROP TABLE IF EXISTS `model_check_params`;
CREATE TABLE `model_check_params` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `model_type` int(255) DEFAULT NULL COMMENT '模型分类：1 资产行为模型 ；2 扫描行为 ；3 Ddos ; 4 加密流量',
  `model_child_type` int(255) DEFAULT NULL COMMENT '模型子类类型：无子类为 0 ；资产行为模型： 1 连接关系模型 ；2 连接频率模型 ； 3 连接流量模型 ； 4 会话信息模型 ； 5 应用分布模型',
  `model_check_switch` int(11) DEFAULT NULL COMMENT '模型检测开关 ： 0 关闭 ，1 打开',
  `model_threshold` int(11) DEFAULT NULL COMMENT '模型检测阈值。只有结果为数值的模型才有检测阈值',
  `model_check_span` int(11) DEFAULT NULL COMMENT '检测所用的模型周期。1 天 2 周 3 季度 4 年',
  `model_check_rate_timeunit` varchar(10) DEFAULT NULL COMMENT '检测频率时间的单位。\r\n秒-ss ；分钟-mm ；小时-hh ；天-dd ；月-MM ；年-yy ；周-ww ; 季度-qq\r\n',
  `model_check_rate_timeunit_num` int(11) DEFAULT NULL COMMENT '检测频率时间的单位数值。\r\n并根据结果选择模型对应的结果。\r\n一个模型可以有多个建模参数形成多个建模结果，但是每次只能有一个检测参数。\r\n根据检测单位、检测频率、模型的类型、子类型、确定模型参数的ID。然后在建模结果中根据建模参数ID找到对应的数据',
  `modeling_params_id` int(11) DEFAULT NULL COMMENT '模型参数ID。根据模型参数ID可以知道建模的类型及子类型',
  `model_check_work_on_weekend` int(11) DEFAULT NULL COMMENT '默认 0 ，无需关注。1 打开 。周末上班，同时期建模参数不使用。需要使用邻近工作日的参数。只针对周模型',
  `model_alarm_level` int(11) DEFAULT NULL COMMENT '告警级别设置 1-5 很低 低 中 高 很高 。0 默认 由算法生成',
  `model_check_alt_params` json DEFAULT NULL COMMENT '模型检测备用参数。',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间。首次编辑添加。当找不到对应的建模结果时应当提示没有结果，请先进行建模。\r\n检测模型实时感知（一分钟一次）检测参数。并在确认和10分钟以后（当前时间减去最后修改时间）开始运行。\r\n当前时间减去最后修改时间小于10分钟的时候，不进行检测。',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
