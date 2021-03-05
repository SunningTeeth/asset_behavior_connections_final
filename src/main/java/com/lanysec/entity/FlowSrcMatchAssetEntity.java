package com.lanysec.entity;

import org.json.simple.JSONObject;

/**
 * @author daijb
 * @date 2021/3/5 21:34
 */
public class FlowSrcMatchAssetEntity {

    /**
     * 资产id
     */
    private String entityId;

    /**
     * 资产ip
     */
    private String assetIp;

    /**
     * flow 中srcIp
     */
    private String srcIp;

    /**
     * flow中srcId
     */
    private String srcId;

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

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    @Override
    public String toString() {
        return "FlowSrcMatchAssetEntity{" +
                "entityId='" + entityId + '\'' +
                ", assetIp='" + assetIp + '\'' +
                ", srcIp='" + srcIp + '\'' +
                ", srcId='" + srcId + '\'' +
                '}';
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("entityId", getEntityId());
        json.put("assetIp", getAssetIp());
        json.put("hostId", getSrcId());
        json.put("hostIp", getSrcIp());
        return json;
    }
}
