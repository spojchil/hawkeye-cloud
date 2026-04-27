package com.hawkeye.asset.common.pojo.DTO;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资产分页查询 DTO
 * <p>
 * 用于 Service 层内部传递分页查询条件，是 {@code PageAssetVO.Request} 到
 * {@code AssetServiceImpl.pageQuery()} 之间的数据传输载体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
