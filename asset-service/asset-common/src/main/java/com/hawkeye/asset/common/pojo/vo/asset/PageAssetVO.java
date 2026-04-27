package com.hawkeye.asset.common.pojo.vo.asset;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import lombok.Data;

/**
 * 资产分页查询 VO
 * <p>
 * Request 用于接收前端分页查询参数（通过 {@code @ParameterObject} 自动展开），
 * Response 用于返回列表项的精简信息（仅包含列表展示需要的字段）。
 */
public class PageAssetVO {

    /**
     * 分页查询请求参数。
     * Controller 通过 {@code @ParameterObject} 将 Query 参数自动绑定到此对象，
     * 前端无需在 Swagger UI 中手动拼 JSON 请求体。
     */
    @Data
    public static class Request {
        private Integer page;
        private Integer pageSize;
        private String name;
        private String requestHost;
        private AssetRiskEnum riskLevel;
        private AssetStatusEnum status;
        private Long categoryId;
    }

    /**
     * 分页列表项返回值（精简字段）。
     * 仅包含列表展示所需的核心字段，完整详情请使用 {@link AssetVO.Response}。
     */
    @Data
    public static class Response {
        private Long assetId;
        private String name;
        private String requestHost;
        private AssetRiskEnum riskLevel;
        private AssetStatusEnum status;
    }
}
