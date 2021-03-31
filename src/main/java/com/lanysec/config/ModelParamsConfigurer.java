package com.lanysec.config;

import com.lanysec.services.AssetBehaviorConstants;
import com.lanysec.utils.ConversionUtil;
import com.lanysec.utils.DbConnectUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author daijb
 * @date 2021/3/5 21:33
 * 模型参数检查
 * TODO 定时更新模型参数待完成
 */
public class ModelParamsConfigurer implements AssetBehaviorConstants {

    private static final Logger logger = LoggerFactory.getLogger(ModelParamsConfigurer.class);

    private static Map<String, Object> modelingParams;

    /**
     * 返回建模参数
     *
     * @return 建模参数k-v
     */
    public static Map<String, Object> getModelingParams() {
        if (modelingParams == null) {
            modelingParams = reloadModelingParams();
        }
        return modelingParams;
    }

    /**
     * 从数据库获取建模参数
     */
    public static Map<String, Object> reloadModelingParams() {
        Map<String, Object> result = new HashMap<>(15 * 3 / 4);
        try {
            Connection connection = DbConnectUtil.getConnection();
            if (connection == null) {
                return result;
            }
            String sql = "select * from modeling_params" +
                    " where model_type=1 and model_child_type =3" +
                    " and model_switch = 1 and model_switch_2 =1";
            //" and modify_time < DATE_SUB( NOW(), INTERVAL 10 MINUTE );";
            ResultSet resultSet = connection.createStatement().executeQuery(sql);
            while (resultSet.next()) {
                result.put(MODEL_ID, resultSet.getString("id"));
                result.put(MODEL_TYPE, resultSet.getString("model_type"));
                result.put(MODEL_CHILD_TYPE, resultSet.getString("model_child_type"));
                result.put(MODEL_RATE_TIME_UNIT, resultSet.getString("model_rate_timeunit"));
                result.put(MODEL_RATE_TIME_UNIT_NUM, resultSet.getString("model_rate_timeunit_num"));
                result.put(MODEL_RESULT_SPAN, resultSet.getString("model_result_span"));
                result.put(MODEL_RESULT_TEMPLATE, resultSet.getString("model_result_template"));
                result.put(MODEL_CONFIDENCE_INTERVAL, resultSet.getString("model_confidence_interval"));
                result.put(MODEL_HISTORY_DATA_SPAN, resultSet.getString("model_history_data_span"));
                result.put(MODEL_UPDATE, resultSet.getString("model_update"));
                result.put(MODEL_SWITCH, resultSet.getString("model_switch"));
                result.put(MODEL_SWITCH_2, resultSet.getString("model_switch_2"));
                result.put(MODEL_ATTRS, resultSet.getString("model_alt_params"));
                result.put(MODEL_TASK_STATUS, resultSet.getString("model_task_status"));
                result.put(MODEL_MODIFY_TIME, resultSet.getString("modify_time"));
            }
        } catch (Throwable throwable) {
            logger.error("Get modeling parameters from the database error ", throwable);
        }
        logger.info("Get modeling parameters from the database : " + result.toString());
        modelingParams = result;
        return result;
    }

    private static volatile Set<String> allAssetIds;

    /**
     * 返回建模参数
     *
     * @return 建模参数k-v
     */
    public static Set<String> getAllAssetIds() throws Exception {
        if (allAssetIds == null) {
            allAssetIds = reloadBuildModelAssetId();
        }
        return allAssetIds;
    }

    /**
     * 重新加载建模资产id
     */
    public static Set<String> reloadBuildModelAssetId() throws SQLException {
        String sql = "SELECT entity_id FROM group_members g,modeling_params m " +
                "WHERE m.model_alt_params -> '$.model_entity_group' LIKE CONCAT('%', g.group_id,'%') " +
                "and m.model_type=1 and model_child_type=3 " +
                "and m.model_switch=1 and m.model_switch_2=1";
        Set<String> result = new HashSet<>();
        Connection conn = DbConnectUtil.getConnection();
        if (conn != null) {
            ResultSet resultSet = conn.prepareStatement(sql).executeQuery();
            while (resultSet.next()) {
                String entityId = ConversionUtil.toString(resultSet.getString("entity_id"));
                result.add(entityId);
            }
        }
        allAssetIds = result;
        return result;
    }


    private static volatile List<Map<String, JSONArray>> lastBuildModelResult = queryLastBuildModelResult();

    public static List<Map<String, JSONArray>> getLastBuildModelResult() {
        if (lastBuildModelResult == null) {
            queryLastBuildModelResult();
        }
        return lastBuildModelResult;
    }

    /**
     * 查询上次建模结果
     */
    public static List<Map<String, JSONArray>> queryLastBuildModelResult() {

        List<Map<String, JSONArray>> result = new ArrayList<>();
        String modelId = ConversionUtil.toString(ModelParamsConfigurer.getModelingParams().get("modelId"));
        String querySql = "select src_id,dst_ip_segment from model_result_asset_behavior_relation " +
                "where modeling_params_id='" + modelId + "';";
        try {
            Connection connection = DbConnectUtil.getConnection();
            if (connection == null) {
                return result;
            }
            ResultSet resultSet = connection.createStatement().executeQuery(querySql);
            while (resultSet.next()) {
                Map<String, JSONArray> map = new HashMap<>();
                String srcId = resultSet.getString("src_id");
                String segmentStr = resultSet.getString("dst_ip_segment");
                JSONArray segmentArr = (JSONArray) JSONValue.parse(segmentStr);
                map.put(srcId, segmentArr);
                result.add(map);
            }
        } catch (SQLException sqlException) {
            logger.error("query build model result failed", sqlException);
        }
        lastBuildModelResult = result;
        return result;
    }
}