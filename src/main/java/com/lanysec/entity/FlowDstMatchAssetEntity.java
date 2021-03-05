package com.lanysec.entity;

import org.json.simple.JSONObject;

/**
 * @author daijb
 * @date 2021/3/5 22:12
 */
public class FlowDstMatchAssetEntity {

    /**
     * 资产id
     */
    private String entityId;

    /**
     * 资产ip
     */
    private String assetIp;

    /**
     * flow 中dstIp
     */
    private String dstIp;

    /**
     * flow中dstId
     */
    private String dstId;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAssetIp() {
        return assetIp;
    }

    public void setAssetIp(String assetIp) {
        this.assetIp = assetIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public String getDstId() {
        return dstId;
    }

    public void setDstId(String dstId) {
        this.dstId = dstId;
    }

    @Override
    public String toString() {
        return "FlowDstMatchAssetEntity{" +
                "entityId='" + entityId + '\'' +
                ", assetIp='" + assetIp + '\'' +
                ", dstIP='" + dstIp + '\'' +
                ", dstId='" + dstId + '\'' +
                '}';
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("entityId", getEntityId());
        json.put("assetIp", getAssetIp());
        json.put("hostId", getDstId());
        json.put("hostIp", getDstIp());
        return json;
    }
}
