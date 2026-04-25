package com.hawkeye.asset.common.pojo.entity;

import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;


import java.time.LocalDateTime;

/**
 * 资产实体 - 继承 BaseEntity 获得通用字段
 * JPA 注解仅用于显式说明表结构，实际映射由 MyBatis XML 管理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @Column(name = "asset_id")
    private Long assetId;

    /**
     * 资产名称
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 请求方法，使用枚举
     */
    @Column(name = "request_method", nullable = false)
    private RequestMethodEnum requestMethod;

    /**
     * 协议
     */
    @Column(name = "request_protocol", nullable = false, length = 10)
    private String requestProtocol;

    /**
     * 请求主机
     */
    @Column(name = "request_host", nullable = false)
    private String requestHost;

    /**
     * 端口号，-1 表示使用协议默认端口
     */
    @Column(name = "request_port", nullable = false)
    private Integer requestPort;

    /**
     * 请求路径
     */
    @Column(name = "request_path", nullable = false, length = 1024)
    private String requestPath;

    /**
     * 请求头，JSON 字符串
     */
    @Column(name = "request_header", columnDefinition = "JSON")
    private String requestHeader;

    /**
     * 资产描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 状态，枚举
     */
    @Column(name = "status", nullable = false)
    private AssetStatusEnum status;

    /**
     * 风险等级，枚举
     */
    @Column(name = "risk_level")
    private AssetRiskEnum riskLevel;

    /**
     * 最近扫描时间
     */
    @Column(name = "last_scan_time")
    private LocalDateTime lastScanTime;

}