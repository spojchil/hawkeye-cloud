package com.hawkeye.asset.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("asset")
public class Asset extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long assetId;
    private String name;
    private RequestMethodEnum requestMethod;
    private String requestProtocol;
    private String requestHost;
    private Integer requestPort;
    private String requestPath;
    private String requestHeader;
    private String description;
    private AssetStatusEnum status;
    private AssetRiskEnum riskLevel;
    private LocalDateTime lastScanTime;
}