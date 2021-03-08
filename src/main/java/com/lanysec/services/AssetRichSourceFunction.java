package com.lanysec.services;

import com.lanysec.utils.DbConnectUtil;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author daijb
 * @date 2021/3/5 21:37
 */
public class AssetRichSourceFunction extends RichSourceFunction<Tuple4<String, String, String, Integer>> {
    private static final long serialVersionUID = 3334654984018091675L;

    private static final Logger logger = LoggerFactory.getLogger(AssetRichSourceFunction.class);

    private PreparedStatement ps;
    private Connection connection;
    private volatile boolean isRunning = true;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        connection = DbConnectUtil.getConnection();
        String sql = "SELECT entity_id, entity_name, asset_ip, area_id " +
                "FROM asset a, modeling_params m " +
                "WHERE m.model_alt_params -> '$.model_entity_group' LIKE CONCAT('%', a.entity_groups,'%') " +
                "and m.model_type=1 and model_child_type=3 " +
                "and model_switch=1 and model_switch_2=1;";
        ps = connection.prepareStatement(sql);
    }

    @Override
    public void run(SourceContext<Tuple4<String, String, String, Integer>> collect) throws Exception {

        try {
            while (isRunning) {
                ResultSet resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    Tuple4<String, String, String, Integer> tuple = new Tuple4<>();
                    tuple.setFields(resultSet.getString(1),
                            resultSet.getString(2),
                            resultSet.getString(3),
                            resultSet.getInt(4)
                    );
                    collect.collect(tuple);
                }
                //每分钟执行一次查询
                Thread.sleep(1000 * 5);
             }
        } catch (Throwable throwable) {
            logger.error("asset rich source failed ", throwable);
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
        } finally {

        }
    }

}
