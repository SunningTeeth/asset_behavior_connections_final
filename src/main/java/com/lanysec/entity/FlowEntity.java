package com.lanysec.entity;

import org.json.simple.JSONObject;

/**
 * @author daijb
 * @date 2021/3/5 21:43
 */
public class FlowEntity {

    private String srcId;
    private String srcIp;
    private String dstId;
    private String dstIp;
    private Integer areaId;
    private String flowId;
    private Long rTime;

    public FlowEntity() {
    }

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getDstId() {
        return dstId;
    }

    public void setDstId(String dstId) {
        this.dstId = dstId;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public void setAreaId(Integer areaId) {
        this.areaId = areaId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public Long getrTime() {
        return rTime;
    }

    public void setrTime(Long rTime) {
        this.rTime = rTime;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("SrcID", getSrcId());
        json.put("SrcIP", getSrcIp());
        json.put("DstID", getDstId());
        json.put("DstIP", getDstIp());
        json.put("AreaID", getAreaId());
        json.put("FlowID", getFlowId());
        json.put("rTime", getrTime());
        return json;
    }

    @Override
    public String toString() {
        return "FlowEntity{" +
                "srcId='" + srcId + '\'' +
                ", srcIp='" + srcIp + '\'' +
                ", dstId='" + dstId + '\'' +
                ", dstIp='" + dstIp + '\'' +
                ", areaId=" + areaId +
                ", flowId='" + flowId + '\'' +
                ", rTime=" + rTime +
                '}';
    }
}
