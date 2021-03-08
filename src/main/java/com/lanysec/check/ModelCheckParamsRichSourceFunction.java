package com.lanysec.check;

import com.lanysec.utils.DbConnectUtil;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author daijb
 * @date 2021/3/6 14:36
 */
public class ModelCheckParamsRichSourceFunction extends RichSourceFunction<Tuple5<String, String, String, String, String>> {

    private PreparedStatement ps;
    private Connection connection;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        connection = DbConnectUtil.getConnection();
        String sql = "SELECT " +
                "c.modeling_params_id,r.dst_ip_segment,r.src_ip,r.src_id,c.model_check_alt_params " +
                "FROM model_result_asset_behavior_relation r,model_check_params c " +
                "WHERE " +
                " r.modeling_params_id = c.modeling_params_id " +
                "AND c.model_type = 1 " +
                "AND c.model_child_type = 3 " +
                "AND c.model_check_switch = 1 " +
                "AND c.modify_time < DATE_SUB( NOW(), INTERVAL 10 MINUTE );";
        ps = connection.prepareStatement(sql);
    }

    @Override
    public void run(SourceContext<Tuple5<String, String, String, String, String>> collect) throws Exception {
        ResultSet resultSet = ps.executeQuery();
        while (resultSet.next()) {
            Tuple5<String, String, String, String, String> tuple = new Tuple5<>();
            tuple.setFields(resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getString(5)
            );
            collect.collect(tuple);
        }
    }

    @Override
    public void cancel() {
        try {
            super.close();
            if (connection != null) {
                connection.close();
            }
            if (ps != null) {
                ps.close();
            }
        } catch (Exception ignored) {
        }
    }
}


