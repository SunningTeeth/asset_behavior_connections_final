package com.lanysec.check;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author daijb
 * @date 2021/3/6 15:49
 */
public class ModelCheckParamsEntity {

    /**
     * 建模结果id
     */
    private String modelingParamsId;

    /**
     * 建模结果资产ip
     */
    private String srcIp;
    /**
     * 建模结果资产id
     */
    private String srcId;

    /**
     * 建模结果资产访问目的ip网段结果集
     */
    private String dstIpSegment;

    /**
     * 模型检测表 扩展字段
     */
    private String modelCheckAltParams;

    public ModelCheckParamsEntity() {
    }

    public String getModelingParamsId() {
        return modelingParamsId;
    }

    public void setModelingParamsId(String modelingParamsId) {
        this.modelingParamsId = modelingParamsId;
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
        return "ModelCheckParamsEntity{" +
                "modelingParamsId='" + modelingParamsId + '\'' +
                ", srcIp='" + srcIp + '\'' +
                ", srcId='" + srcId + '\'' +
                ", dstIpSegment=" + dstIpSegment +
                ", modelCheckAltParams=" + modelCheckAltParams +
                '}';
    }
}
