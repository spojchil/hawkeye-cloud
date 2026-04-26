package com.hawkeye.asset.common.pojo.entity;

import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 资产实体 - 继承 BaseEntity 获得通用字段
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Asset extends BaseEntity {

    // TODO 分布式不建议使用自增的主键id,之后改吧
    /**
     * 主键ID
     */
    private Long assetId;

    /**
     * 资产名称
     */
    private String name;

    /**
     * 请求方法，使用枚举
     */
    private RequestMethodEnum requestMethod;

    /**
     * 协议
     */
    private String requestProtocol;

    /**
     * 请求主机
     */
    private String requestHost;

    /**
     * 端口号，-1 表示使用协议默认端口
     */
    private Integer requestPort;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求头，JSON 字符串
     */
    private String requestHeader;

    /**
     * 资产描述
     */
    private String description;

    /**
     * 状态，枚举
     */
    private AssetStatusEnum status;

    /**
     * 风险等级，枚举
     */
    private AssetRiskEnum riskLevel;

    /**
     * 最近扫描时间
     */
    private LocalDateTime lastScanTime;

}