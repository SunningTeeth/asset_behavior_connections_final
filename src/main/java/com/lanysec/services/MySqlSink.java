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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author daijb
 * @date 2021/3/5 21:37
 */
public class MySqlSink extends RichSinkFunction<JSONObject> {

    private static final Logger logger = LoggerFactory.getLogger(MySqlSink.class);

    private PreparedStatement ps;

    private Connection connection;

    private volatile ServiceState state = ServiceState.Starting;

    private volatile boolean isFirst = true;

    private final Lock modelTaskStatusLock = new ReentrantLock();

    private volatile Map<String, Object> modelingParams = ModelParamsConfigurer.getModelingParams();

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        logger.info("mysql sink connection is opening....");
        connection = DbConnectUtil.getConnection();
        String sql = "insert into `model_result_asset_behavior_relation` (`id`,`modeling_params_id`,`src_id`,`src_ip`,`dst_ip_segment`,`time`)" +
                " values (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `dst_ip_segment`=?";
        ps = connection.prepareStatement(sql);
    }

    @Override
    public void invoke(JSONObject json, Context context) throws Exception {
        checkModelInfo();
        checkState();
        if (state != ServiceState.Ready) {
            return;
        }
        String key = ConversionUtil.toString(calculateSegmentCurrKey());
        String modelId = ModelParamsConfigurer.getModelingParams().get("modelId").toString();
        String querySql = "select * from model_result_asset_behavior_relation where modeling_params_id='" + modelId + "';";
        PreparedStatement preparedStatement = connection.prepareStatement(querySql);
        ResultSet resultSet = preparedStatement.executeQuery();
        String oldSegmentStr = "";
        while (resultSet.next()) {
            oldSegmentStr = resultSet.getString("dst_ip_segment");
        }
        JSONArray segmentArr = null;
        String hostIp = json.get("hostIp").toString();
        if (!StringUtil.isEmpty(oldSegmentStr)) {
            JSONArray segmentArrTemp = (JSONArray) JSONValue.parse(oldSegmentStr);
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
        } else {
            segmentArr = new JSONArray();
            JSONObject item = new JSONObject();
            JSONArray ips = new JSONArray();
            ips.add(hostIp);
            item.put(key, ips);
            segmentArr.add(item);
        }
        ps.setString(1, "mdl_" + UUIDUtil.genId());
        ps.setString(2, modelId);
        ps.setString(3, json.get("entityId").toString());
        ps.setString(4, getAssetOfIp(json.get("assetIp").toString()));
        ps.setString(5, segmentArr.toJSONString());
        ps.setString(6, LocalDateTime.now().toString());
        ps.setString(7, segmentArr.toJSONString());
        ps.execute();
        if (isFirst) {
            isFirst = false;
            updateModelTaskStatus(AssetBehaviorConstants.ModelStatus.SUCCESS);
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
        int start = assetIp.indexOf("[") + 1;
        int end = assetIp.indexOf("]");
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
            DbConnectUtil.execUpdateTask(updateSql, modelStatus.toString().toLowerCase(), LocalDateTime.now().toString());
            logger.info("update model task status : " + modelStatus.name());
        } finally {
            modelTaskStatusLock.unlock();
        }
    }

    /**
     * 检查模型各种信息
     */
    private void checkModelInfo() {
        Map<String, Object> newModelingParams = ModelParamsConfigurer.getModelingParams();
        if (newModelingParams.isEmpty()) {
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

        //建模维度变化
        boolean modelParamsChange = modelParamsChange(newModelingParams);
        if (modelParamsChange) {

        }

        // 建模数据存储历史天数
        boolean modelHistoryDataDaysChange = checkModelHistoryDataDays(newModelingParams);
        if (modelHistoryDataDaysChange) {

        }
        this.modelingParams = newModelingParams;
    }

    /**
     * 检查模型开关
     */
    private void checkState() {
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
            return true;
        }
        return false;

    }


}

