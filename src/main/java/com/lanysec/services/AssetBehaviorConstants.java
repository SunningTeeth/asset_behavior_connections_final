package com.lanysec.services;

/**
 * @author daijb
 * @date 2021/3/5 21:35
 */
public interface AssetBehaviorConstants {

    /**
     * 模型ID
     */
    public static final String MODEL_ID = "modelId";
    /**
     * 模型分类：1 资产行为模型 ；2 扫描行为 ；3 Ddos ; 4 加密流量
     */
    public static final String MODEL_TYPE = "modelType";
    /**
     * 模型子类类型：无子类为 0 ；资产行为模型： 1 连接关系模型 ；2 连接频率模型 ； 3 连接流量模型 ； 4 会话信息模型 ； 5 应用分布模型
     */
    public static final String MODEL_CHILD_TYPE = "modelChildType";
    /**
     * 模型建模的频率的时间单位：秒-ss ；分钟-mm ；小时-hh ；天-dd ；月-MM ；年-yy ；周-ww ; 季度-qq
     */
    public static final String MODEL_RATE_TIME_UNIT = "modelRateTimeUnit";
    /**
     * 建模频率时间单位对应的数值。单位为 mm ；数值为 5；代表5分钟一次结果。结合上面参数使用。现在只支持 1mm 5mm 1hh 1dd
     */
    public static final String MODEL_RATE_TIME_UNIT_NUM = "modelRateTimeUnitNum";
    /**
     * 建模结果周期：1 代表一天。2 代表一周。3 代表一季度。 4 代表一年。如果总长度填写 1 ，建模的时间单位可以是 ss mm hh 。周的建模时间单位只能是 dd 。其他的只能为月
     */
    public static final String MODEL_RESULT_SPAN = "modelResultSpan";
    /**
     * 模型结果模板 json格式：如果建模总时间长度为1周{1:10;2:11;3:1;4:2;5:2;6:10;7:10}
     */
    public static final String MODEL_RESULT_TEMPLATE = "model_result_template";
    /**
     * 置信度。范围 0-1 。模型参数：样本总体方差 模型中计算求得。根据置信度，查询标准正态分布表。计算样本的置信区间\r\n不是所有模型都有置信区间。置信度选择直接影响模型结果
     */
    public static final String MODEL_CONFIDENCE_INTERVAL = "model_confidence_interval";
    /**
     * 所需历史数据 最少为1天。只能是1的整数倍
     */
    public static final String MODEL_HISTORY_DATA_SPAN = "model_history_data_span";
    /**
     * 模型的更新方式。0 清除历史数据更新 1 累计迭代历史数据更新。模型结果的唯一性：\r\n1.模型分类\r\n2.模型子类\r\n3.频率\r\n4.频数\r\n5.总时间长度\r\n更改上面的内容。新增。\r\n更改 历史数据和置信度 更新。
     */
    public static final String MODEL_UPDATE = "model_update";
    /**
     * 0 关闭 1 打开。总开关。页面控制
     */
    public static final String MODEL_SWITCH = "model_switch";
    /**
     * 编辑控制。一种模型的多套参数同时只能有一个是开启状态。0 关闭 1 打开。
     */
    public static final String MODEL_SWITCH_2 = "model_switch_2";
    /**
     * 扩展参数
     */
    public static final String MODEL_ATTRS = "model_alt_params";
    /**
     * 建模的状态。参数选定：prepare ；提交 flink 任务：running；建模成功：success；建模失败：failed；意外终止：sto
     */
    public static final String MODEL_TASK_STATUS = "model_task_status";
    /**
     * 最后修改时间。首次编辑添加
     */
    public static final String MODEL_MODIFY_TIME = "modify_time";
    /**
     * 更新间隔
     */
    public final long INTERVAL = 60 * 60 * 1000;
    /**
     * persistence
     */
    public static final String PERSISTENCE_FILEPATH = "/usr/csp/model/assetBehavior/";

    static enum ModelStatus {
        /**
         * 建模的状态。参数选定：prepare
         */
        PREPARE,
        /**
         * 提交Flink任务：running
         */
        RUNNING,
        /**
         * 建模成功：success
         */
        SUCCESS,
        /**
         * 建模失败：failed
         */
        FAILED,
        /**
         * 意外终止：stop
         */
        STOP
    }

    static enum ModelCycle {
        /**
         * 小时
         */
        HOURS,
        /**
         * 天
         */
        DAYS,
        /**
         * 周
         */
        WEEK,
        /**
         * 月
         */
        MONTH,
        /**
         * 季度
         */
        QUARTER,
        /**
         * 年
         */
        YEAR
    }
}
