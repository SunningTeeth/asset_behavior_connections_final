package com.lanysec.services;

import com.lanysec.config.ModelParamsConfigurer;
import com.lanysec.utils.ConversionUtil;
import com.lanysec.utils.DbConnectUtil;
import com.lanysec.utils.StringUtil;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

/**
 * @author daijb
 * @date 2021/3/7 15:22
 * 检测
 */
public class CheckModelMapSourceFunction extends RichMapFunction<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(CheckModelMapSourceFunction.class);

    private Connection connection;

    /**
     * 存储当前模型建模结果
     * key : modelingParamsId
     * value : k: srcId ===> v : 一条记录
     */
    private List<Map<String, Object>> modelResults = new ArrayList<>();

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        connection = DbConnectUtil.getConnection();
        String sql = "SELECT " +
                "c.modeling_params_id,r.dst_ip_segment,r.src_ip,r.src_id,c.model_check_alt_params " +
                "FROM model_result_asset_behavior_relation r,model_check_params c " +
                "WHERE " +
                "r.modeling_params_id = c.modeling_params_id " +
                "AND c.model_type = 1 " +
                "AND c.model_child_type = 3 " +
                "AND c.model_check_switch = 1 " +
                "AND c.modify_time < DATE_SUB( NOW(), INTERVAL 10 MINUTE );";

        ResultSet resultSet = connection.prepareStatement(sql).executeQuery();
        while (resultSet.next()) {
            Map<String, Object> map = new HashMap<>(5);
            String modelingParamsId = ConversionUtil.toString(resultSet.getString("modeling_params_id"));
            String dstIpSegment = ConversionUtil.toString(resultSet.getString("dst_ip_segment"));
            String srcIp = ConversionUtil.toString(resultSet.getString("src_ip"));
            String srcId = ConversionUtil.toString(resultSet.getString("src_id"));
            String modelCheckAltParams = ConversionUtil.toString(resultSet.getString("model_check_alt_params"));
            map.put("modelingParamsId", modelingParamsId);
            map.put("dstIpSegment", dstIpSegment);
            map.put("srcIp", srcIp);
            map.put("srcId", srcId);
            map.put("modelCheckAltParams", modelCheckAltParams);
            modelResults.add(map);
        }
    }

    @Override
    public String map(String line) throws Exception {
        //{"L7P":"tls","InPackets":20,"ClusterID":"EWN7S4UJ","L3P":"IP","OutFlow":1350,"OutPackets":10,"OutFlags":0,"InFlow":1950,"rHost":"192.168.9.58","SrcID":"ast_458b6b75b0610d081dc83c7c6a34498a","ID":"fle_TTJoPs5YQoTQwZqGbkfvmG","sTime":1615090860000,"SrcCountry":"中国北京","rType":"1","SrcPort":17339,"DstLocName":"杭州","eTime":1615090981000,"AreaID":19778692,"L4P":"TCP","DstCountry":"中国","InFlags":0,"MetaID":"evm_flow","DstPort":443,"SrcIP":"192.168.7.249","ESMetaID":"esm_flow","SID":"1296f58e4f3ab1c3ced4bce072532608","FlowID":"1296f58e4f3ab1c3ced4bce072532608","rTime":1615090981139,"@timestamp":1615090995592,"DstID":"","DstIP":"47.111.111.35","DstMAC":"bc:3f:8f:63:6c:80","PcapID":"","TrafficSource":"eth1","SrcMAC":"4c:cc:6a:57:95:8a","Key":"","SrcLocName":"未知"}
        JSONObject json = (JSONObject) JSONValue.parse(line);
        String srcId = ConversionUtil.toString(json.get("SrcID"));
        String srcIp = ConversionUtil.toString(json.get("SrcIP"));
        String dstId = ConversionUtil.toString(json.get("DstID"));
        String dstIp = ConversionUtil.toString(json.get("DstIP"));
        for (Map<String, Object> map : modelResults) {

            //TODO 排除白名单 放行
            JSONObject obj = (JSONObject) JSONValue.parse(ConversionUtil.toString(map.get("modelCheckAltParams")));
            Object whiteIps = obj.get("white_ip_list");
            if (whiteIps != null) {
                JSONArray whiteIpsList = (JSONArray) JSONValue.parse(whiteIps.toString());
                if (whiteIpsList != null && !whiteIpsList.isEmpty()) {
                    Set whileIpSet = new HashSet(whiteIpsList);
                    if (whileIpSet.contains(srcIp)) {
                        return null;
                    }
                    if (whileIpSet.contains(dstIp)) {
                        return null;
                    }
                }
            }

            String entityId = ConversionUtil.toString(map.get("srcId"));
            String dstIpSegmentStr = ConversionUtil.toString(map.get("dstIpSegment"));
            if (StringUtil.isEmpty(dstIpSegmentStr)) {
                return line;
            }
            JSONArray dstIpSegmentArr = (JSONArray) JSONValue.parse(dstIpSegmentStr);
            if (dstIpSegmentArr == null || dstIpSegmentArr.isEmpty()) {
                return line;
            }
            //TODO 检测对应资产连接的目的IP是否在建模的网段
            //TODO 分时统计的,如何检测？？？
            Set set = new HashSet(dstIpSegmentArr);
            if (StringUtil.equalsIgnoreCase(entityId, srcId)) {
                if (set.contains(dstIp)) {
                    return line;
                }
            }
            if (StringUtil.equalsIgnoreCase(entityId, dstId)) {
                if (set.contains(srcIp)) {
                    return line;
                }
            }
        }
        return null;
    }
}