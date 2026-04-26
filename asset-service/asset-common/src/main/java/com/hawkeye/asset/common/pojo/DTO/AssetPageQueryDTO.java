package com.hawkeye.asset.common.pojo.DTO;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class AssetPageQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer page;
    private Integer pageSize;
    private String name;
    private String requestHost;
    private AssetRiskEnum riskLevel;
    private AssetStatusEnum status;
    private Long categoryId;
}
