package com.lanysec.services;

import com.lanysec.config.ModelParamsConfigurer;
import com.lanysec.utils.ConversionUtil;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Set;

/**
 * @author daijb
 * @date 2021/3/7 12:12
 */
public class AssetMapSourceFunction extends RichMapFunction<String, String> {

    @Override
    public String map(String line) throws Exception {
        //{"L7P":"tls","InPackets":20,"ClusterID":"EWN7S4UJ","L3P":"IP","OutFlow":1350,"OutPackets":10,"OutFlags":0,"InFlow":1950,"rHost":"192.168.9.58","SrcID":"ast_458b6b75b0610d081dc83c7c6a34498a","ID":"fle_TTJoPs5YQoTQwZqGbkfvmG","sTime":1615090860000,"SrcCountry":"中国北京","rType":"1","SrcPort":17339,"DstLocName":"杭州","eTime":1615090981000,"AreaID":19778692,"L4P":"TCP","DstCountry":"中国","InFlags":0,"MetaID":"evm_flow","DstPort":443,"SrcIP":"192.168.7.249","ESMetaID":"esm_flow","SID":"1296f58e4f3ab1c3ced4bce072532608","FlowID":"1296f58e4f3ab1c3ced4bce072532608","rTime":1615090981139,"@timestamp":1615090995592,"DstID":"","DstIP":"47.111.111.35","DstMAC":"bc:3f:8f:63:6c:80","PcapID":"","TrafficSource":"eth1","SrcMAC":"4c:cc:6a:57:95:8a","Key":"","SrcLocName":"未知"}
        JSONObject json = (JSONObject) JSONValue.parse(line);
        String srcId = ConversionUtil.toString(json.get("SrcID"));
        String srcIp = ConversionUtil.toString(json.get("SrcIP"));
        String dstIp = ConversionUtil.toString(json.get("DstIP"));
        JSONObject result = new JSONObject();
        Set<String> allAssetIds = ModelParamsConfigurer.getAllAssetIds();
        if (allAssetIds.contains(srcId)) {
            result.put("entityId", srcId);
            result.put("assetIp", srcIp);
            // 资产连接的目标ip
            result.put("hostIp", dstIp);
            return result.toJSONString();
        }
        return null;
    }
}
