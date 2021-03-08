package com.lanysec.entity;

/**
 * @author daijb
 * @date 2021/3/5 21:44
 */
public class AssetSourceEntity {

    private String entityId;
    private String entityName;
    private String assetIp;
    private Integer areaId;

    public AssetSourceEntity() {
    }

    public AssetSourceEntity(String entityId, String entityName, String assetIp, Integer areaId) {
        this.entityId = entityId;
        this.entityName = entityName;
        this.assetIp = assetIp;
        this.areaId = areaId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getAssetIp() {
        return assetIp;
    }

    public void setAssetIp(String assetIp) {
        this.assetIp = assetIp;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public void setAreaId(Integer areaId) {
        this.areaId = areaId;
    }

    @Override
    public String toString() {
        return "AssetSourceEntity{" +
                "entityId='" + entityId + '\'' +
                ", entityName='" + entityName + '\'' +
                ", assetIp='" + assetIp + '\'' +
                ", areaId=" + areaId +
                '}';
    }
}
