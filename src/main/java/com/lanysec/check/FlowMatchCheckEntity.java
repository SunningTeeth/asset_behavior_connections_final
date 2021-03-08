package com.lanysec.check;

import org.json.simple.JSONObject;

/**
 * @author daijb
 * @date 2021/3/6 16:18
 */
public class FlowMatchCheckEntity {

    private String srcIp;
    private String srcPort;
    private String srcId;
    private String dstIp;
    private String dstPort;
    private String dstId;
    private String id;
    private String l4p;
    private String l7p;
    private String dstIpSegment;
    private String modelCheckAltParams;

    public FlowMatchCheckEntity() {
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(String srcPort) {
        this.srcPort = srcPort;
    }

    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public String getDstPort() {
        return dstPort;
    }

    public void setDstPort(String dstPort) {
        this.dstPort = dstPort;
    }

    public String getDstId() {
        return dstId;
    }

    public void setDstId(String dstId) {
        this.dstId = dstId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getL4p() {
        return l4p;
    }

    public void setL4p(String l4p) {
        this.l4p = l4p;
    }

    public String getL7p() {
        return l7p;
    }

    public void setL7p(String l7p) {
        this.l7p = l7p;
    }

    public String getDstIpSegment() {
        return dstIpSegment;
    }

    public void setDstIpSegment(String dstIpSegment) {
        this.dstIpSegment = dstIpSegment;
    }

    public String getModelCheckAltParams() {
        return modelCheckAltParams;
    }

    public void setModelCheckAltParams(String modelCheckAltParams) {
        this.modelCheckAltParams = modelCheckAltParams;
    }

    @Override
    public String toString() {
        return "FlowSrcMatchCheckEntity{" +
                "srcIp='" + srcIp + '\'' +
                ", srcPort='" + srcPort + '\'' +
                ", srcId='" + srcId + '\'' +
                ", dstIp='" + dstIp + '\'' +
                ", dstPort='" + dstPort + '\'' +
                ", dstId='" + dstId + '\'' +
                ", id='" + id + '\'' +
                ", l4p='" + l4p + '\'' +
                ", l7p='" + l7p + '\'' +
                ", dstIpSegment=" + dstIpSegment +
                ", modelCheckAltParams=" + modelCheckAltParams +
                '}';
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("ID",getId());
        json.put("SrcID",getSrcId());
        json.put("SrcIP",getSrcIp());
        json.put("SrcPort",getSrcPort());
        json.put("DstID",getDstId());
        json.put("DstIP",getDstIp());
        json.put("DstPort",getDstPort());
        json.put("L4P",getL4p());
        json.put("L7P",getL7p());

        return json;
    }
}
