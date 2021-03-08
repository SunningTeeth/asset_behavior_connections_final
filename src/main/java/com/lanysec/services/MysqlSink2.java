package com.lanysec.services;

import com.lanysec.config.ModelParamsConfigurer;
import com.lanysec.utils.ConversionUtil;
import com.lanysec.utils.DbConnectUtil;
import com.lanysec.utils.StringUtil;
import com.lanysec.utils.UUIDUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author daijb
 * @date 2021/3/7 13:49
 */
public class MysqlSink2 extends RichSinkFunction<String> {

    private static final Logger logger = LoggerFactory.getLogger(MysqlSink2.class);

    private PreparedStatement ps;

    private Connection connection;

    private volatile ServiceState state = ServiceState.Starting;

    private volatile boolean isFirst = true;

    /**
     * 记录历史数据天数
     */
    private volatile int historyDataDays = 1;

    /**
     * 程序最多执行时间
     */
    private volatile Date futureDate;

    private final Lock modelTaskStatusLock = new ReentrantLock();

    private volatile Map<String, Object> modelingParams = ModelParamsConfigurer.getModelingParams();

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        connection = DbConnectUtil.getConnection();
        String sql = "insert into `model_result_asset_behavior_relation` (`id`,`modeling_params_id`,`src_id`,`src_ip`,`dst_ip_segment`,`time`)" +
                " values (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `dst_ip_segment`=?";
        ps = connection.prepareStatement(sql);
        logger.info("mysql sink connection is opening....");
    }

    @Override
    public void invoke(String jsonStr, Context context) throws Exception {

        checkModelInfo();
        checkState();
        if (state != ServiceState.Ready) {
            logger.warn("build modeling is stopped...");
            return;
        }
        // 判断是否超出存储数据天数
        if (!isFirst && !canSink()) {
            logger.warn("more than save data days");
            return;
        }
        JSONObject json = (JSONObject) JSONValue.parse(jsonStr);
        String key = ConversionUtil.toString(calculateSegmentCurrKey());
        String modelId = ModelParamsConfigurer.getModelingParams().get("modelId").toString();
        String entityId = ConversionUtil.toString(json.get("entityId"));
        String assetIp = ConversionUtil.toString(json.get("assetIp"));
        logger.info("-- : " + json + "\n modelId : " + modelId +
                "\n entityId : " + entityId + "\n assetIp:" + assetIp + "\n key : " + key);
        JSONArray segmentArr = null;
        String hostIp = json.get("hostIp").toString();
        List<Map<String, JSONArray>> lastBuildModelResult = ModelParamsConfigurer.getLastBuildModelResult();
        for (Map<String, JSONArray> v : lastBuildModelResult) {
            for (Map.Entry<String, JSONArray> entry : v.entrySet()) {
                if (!StringUtil.equals(entityId, entry.getKey())) {
                    continue;
                }
                JSONArray segmentArrTemp = entry.getValue();
                JSONArray temp = new JSONArray();
                for (Object obj : segmentArrTemp) {
                    JSONObject item = (JSONObject) JSONValue.parse(obj.toString());
                    Set set = new HashSet();
                    if (item.get(key) != null) {
                        set.addAll((JSONArray) JSONValue.parse(item.get(key).toString()));
                    }
                    set.add(hostIp);
                    JSONArray arr = new JSONArray();
                    arr.addAll(set);
                    item.put(key, arr);
                    temp.add(item);
                }
                segmentArr = temp;
            }
        }
        if (segmentArr == null || segmentArr.isEmpty()) {
            segmentArr = new JSONArray();
            JSONObject item = new JSONObject();
            JSONArray ips = new JSONArray();
            ips.add(hostIp);
            item.put(key, ips);
            segmentArr.add(item);
        }
        ps.setString(1, "mdl_" + UUIDUtil.genId());
        ps.setString(2, modelId);
        ps.setString(3, entityId);
        ps.setString(4, getAssetOfIp(assetIp));
        ps.setString(5, segmentArr.toJSONString());
        ps.setString(6, LocalDateTime.now().toString());
        ps.setString(7, segmentArr.toJSONString());
        ps.executeUpdate();
        if (isFirst) {
            isFirst = false;
            updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.SUCCESS);
            //记录运行天数
            updateModelHistoryDataDays();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + this.historyDataDays);
            this.futureDate = calendar.getTime();
        }

    }

    @Override
    public void close() throws Exception {
        super.close();
        if (connection != null) {
            connection.close();
        }
        if (ps != null) {
            ps.close();
        }
    }

    /**
     * 建模结果周期：1 代表一天。
     * 2 代表一周。3 代表一季度。
     * 4 代表一年。如果总长度填写 1 ，建模的时间单位可以是 ss mm hh 。周的建模时间单位只能是 dd 。其他的只能为月
     * 计算模型的key
     * <pre>
     *   1. 建模周期为天 ：
     *       SegmentKey : 每次从当前日期开始计算,以小时为key
     *   2. 建模周期为周：建模时间单位只能是天）
     *       SegmentKey : 每次从当前日期开始计算,以当前周几为key
     *   3. 建模周期为季度：（建模时间单位只能是月）
     *      SegmentKey : 每次从当前日期开始计算,以当前月份开始递增
     *   4. 建模周期为年：（建模时间单位只能是月）
     *      SegmentKey : 每次从当前日期开始计算,以当前月份开始递增
     *
     * </pre>
     */
    private Object calculateSegmentCurrKey() throws Exception {
        // 建模周期
        int cycle = ConversionUtil.toInteger(ModelParamsConfigurer.getModelingParams().get(AssetBehaviorConstants.MODEL_RESULT_SPAN));
        LocalDateTime now = LocalDateTime.now();
        Object segmentKey = null;
        switch (cycle) {
            // 暂时默认为小时
            case 1: {
                segmentKey = now.getHour();
                break;
            }
            //周,频率只能是 dd
            case 2: {
                segmentKey = now.getDayOfWeek().getValue();
                break;
            }
            //季度,频率只能是月
            case 3:
            case 4: {
                // 年,频率只能是月
                segmentKey = now.getMonth().getValue();
                break;
            }
            default: {
                throw new Exception("modeling span is not support.");
            }
        }
        return segmentKey;
    }

    private String getAssetOfIp(String assetIp) {
        //  {"00:00:00:00:00:00":["192.168.8.97"]}
        int start = assetIp.indexOf("[") + 2;
        int end = assetIp.indexOf("]") - 1;
        return assetIp.substring(start, end);
    }

    /**
     * 更新建模状态
     *
     * @param modelStatus 状态枚举
     */
    private void updateModelTaskStatus(AssetBehaviorConstants.ModelStatus modelStatus) {
        while (!modelTaskStatusLock.tryLock()) {
        }
        try {
            String updateSql = "UPDATE `modeling_params` SET `model_task_status`=?, `modify_time`=? WHERE (`id`='" + modelingParams.get(AssetBehaviorConstants.MODEL_ID) + "');";
            PreparedStatement ps = connection.prepareStatement(updateSql);
            ps.setString(1, modelStatus.toString().toLowerCase());
            ps.setString(2, LocalDateTime.now().toString());
            ps.execute();
            logger.info("update model task status : " + modelStatus.name());
        } catch (Throwable t) {
        } finally {
            modelTaskStatusLock.unlock();
        }
    }

    /**
     * 更新模型建模数据存储历史天数
     */
    private void updateModelHistoryDataDays() {
        String sql = "INSERT INTO `config` (`keyword`, `vals`, `opts`, `types`, `name`, `notes`, `gid`, `sys`, `sort`) VALUES " +
                "('ASSET_BEHAVIOR_CONNECTION', ?, '', 'int', '资产行为连接关系记录存储数据天数', '', '0', '1', '50') " +
                "ON DUPLICATE KEY UPDATE `vals`=?";

        int day = this.historyDataDays;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, day + "");
            preparedStatement.setString(2, day + "");
            if (preparedStatement.execute()) {
                logger.info("update model history data days : " + day);
            }
        } catch (Throwable throwable) {
            logger.error("update model history data days : " + day + " failed.", throwable);
        }

    }

    /**
     * 检查模型各种信息
     */
    private void checkModelInfo() {
        Map<String, Object> newModelingParams = ModelParamsConfigurer.getModelingParams();
        if (newModelingParams.isEmpty()) {
            state = ServiceState.Stopped;
            return;
        }

        //建模维度变化
        boolean modelParamsChange = modelParamsChange(newModelingParams);
        if (modelParamsChange) {
            // 说明此时出现：同一个模型 多个不同的维度(设计要求，同一个模型只能有一个维度生效)
            state = ServiceState.Stopped;
            updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.STOP);
            //更新建模参数
            ModelParamsConfigurer.reloadModelingParams();
            this.modelingParams = ModelParamsConfigurer.getModelingParams();
            return;
        }

        boolean updateChange = modelUpdateChange(newModelingParams);
        if (updateChange) {
            // 更新方式发生变化
            // 模型的更新方式:
            // 0(false)清除历史数据更新
            // 1(true)累计迭代历史数据更新。
            // 模型结果的唯一性:模型分类 & 模型子类 & 频率 & 频数
            // 更改 历史数据和置信度 更新。
            boolean modelUpdate = ConversionUtil.toBoolean(newModelingParams.get(AssetBehaviorConstants.MODEL_UPDATE));
            if (!modelUpdate) {
                // 清除历史数据更新
                if (connection == null) {
                    connection = DbConnectUtil.getConnection();
                }
                String deleteSql = "DELETE FROM model_result_asset_behavior_relation WHERE modeling_params_id ='"
                        + newModelingParams.get(AssetBehaviorConstants.MODEL_ID).toString() + "';";
                try {
                    boolean result = connection.createStatement().execute(deleteSql);
                } catch (SQLException sqlException) {
                    updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.FAILED);
                    logger.error("exec clear history data failed.", sqlException);
                }
            }
        }

        // 建模数据存储历史天数
        boolean modelHistoryDataDaysChange = checkModelHistoryDataDays(newModelingParams);
        if (modelHistoryDataDaysChange) {
            updateModelHistoryDataDays();
        }
        this.modelingParams = newModelingParams;
    }

    /**
     * 检查模型开关
     */
    private void checkState() {
        if (this.modelingParams.isEmpty()) {
            this.modelingParams = ModelParamsConfigurer.getModelingParams();
            state = ServiceState.Stopped;
            // 更新状态
            updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.FAILED);
            return;
        }
        if (ConversionUtil.toBoolean(this.modelingParams.get(AssetBehaviorConstants.MODEL_SWITCH))) {
            state = ServiceState.Ready;
        } else {
            state = ServiceState.Stopped;
            // 更新状态
            updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.FAILED);
        }
    }

    /**
     * 更新方式是否发生变化
     */
    private boolean modelUpdateChange(Map<String, Object> newModelingParams) {

        // 更新方式
        Integer newModelUpdate = ConversionUtil.toInteger(newModelingParams.get(AssetBehaviorConstants.MODEL_UPDATE));
        Integer oldModelUpdate = ConversionUtil.toInteger(modelingParams.get(AssetBehaviorConstants.MODEL_UPDATE));
        if (newModelUpdate != null && !newModelUpdate.equals(oldModelUpdate)) {
            return true;
        }
        return false;
    }

    /**
     * 定期检查建模维度是否发生变化
     * 当前维度的定义： 建模周期 & 模型建模的频率 & 建模频率时间单位对应的数值
     */
    private boolean modelParamsChange(Map<String, Object> newModelingParams) {

        Map<String, Object> modelingParams = this.modelingParams;
        // 建模周期 model_result_span
        boolean modelResultSpanFlag = false;
        Integer newModelResultSpan = ConversionUtil.toInteger(newModelingParams.get(AssetBehaviorConstants.MODEL_RESULT_SPAN));
        Integer oldModelResultSpan = ConversionUtil.toInteger(modelingParams.get(AssetBehaviorConstants.MODEL_RESULT_SPAN));
        if (newModelResultSpan != null && !newModelResultSpan.equals(oldModelResultSpan)) {
            modelResultSpanFlag = true;
        }

        // 模型建模的频率 model_rate_timeunit
        boolean modelRateTimeUnitFlag = false;
        String newModelRateTimeUnit = ConversionUtil.toString(newModelingParams.get(AssetBehaviorConstants.MODEL_RATE_TIME_UNIT));
        String oldModelRateTimeUnit = ConversionUtil.toString(modelingParams.get(AssetBehaviorConstants.MODEL_RATE_TIME_UNIT));
        if (StringUtil.equals(newModelRateTimeUnit, oldModelRateTimeUnit)) {
            modelRateTimeUnitFlag = true;
        }

        // 建模频率时间单位对应的数值 model_rate_timeunit_num
        boolean modelRateTimeUnitNumFlag = false;
        Integer newModelRateTimeUnitNum = ConversionUtil.toInteger(newModelingParams.get(AssetBehaviorConstants.MODEL_RATE_TIME_UNIT_NUM));
        Integer oldModelRateTimeUnitNum = ConversionUtil.toInteger(modelingParams.get(AssetBehaviorConstants.MODEL_RATE_TIME_UNIT_NUM));
        if (newModelRateTimeUnitNum != null && !newModelRateTimeUnitNum.equals(oldModelRateTimeUnitNum)) {
            modelRateTimeUnitNumFlag = true;
        }

        if (modelResultSpanFlag && modelRateTimeUnitFlag && modelRateTimeUnitNumFlag) {
            return true;
        }

        return false;
    }

    /**
     * 检查模型所需历史天数
     *
     * @param newModelingParams 模型参数
     * @return true : 历史天数发送改变
     */
    private boolean checkModelHistoryDataDays(Map<String, Object> newModelingParams) {

        // 所需历史天数 model_history_data_span
        Integer newModelHistoryData = ConversionUtil.toInteger(newModelingParams.get(AssetBehaviorConstants.MODEL_HISTORY_DATA_SPAN));
        Integer oldModelHistoryData = ConversionUtil.toInteger(modelingParams.get(AssetBehaviorConstants.MODEL_HISTORY_DATA_SPAN));
        if (newModelHistoryData != null && !newModelHistoryData.equals(oldModelHistoryData)) {
            this.historyDataDays = ConversionUtil.toInteger(newModelingParams.get(AssetBehaviorConstants.MODEL_HISTORY_DATA_SPAN));
            return true;
        }
        return false;
    }

    /**
     * 判断time是否在now的n天之内
     * n :正数表示在条件时间n天之后，负数表示在条件时间n天之前
     */
    private boolean canSink() {
        Date cTime = new Date(), fTime = this.futureDate;
        int n = -this.historyDataDays;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cTime);
        calendar.add(Calendar.DAY_OF_MONTH, n);
        //得到n天前的时间
        Date before7days = calendar.getTime();
        if (before7days.getTime() <= fTime.getTime()) {
            return true;
        } else {
            return false;
        }
    }
}
