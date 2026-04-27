package com.hawkeye.asset.common.pojo.vo.asset;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产 VO（视图对象）
 * <p>
 * 包含 Request（创建/更新请求体）和 Response（返回值）两个内部静态类，
 * 遵循项目 VO 命名约定，避免创建过多独立类文件。
 */
public class AssetVO {

    /**
     * 资产创建/更新请求体。
     * 包含 Jakarta Validation 约束注解，在 Controller 层通过 {@code @Valid} 触发校验。
     */
    @Data
    public static class Request {
        @NotBlank(message = "资产名称不能为空")
        private String name;

        @NotNull(message = "请求方法不能为空")
        private RequestMethodEnum requestMethod;

        @NotBlank(message = "协议不能为空")
        private String requestProtocol;

        @NotBlank(message = "请求主机不能为空")
        private String requestHost;

        @NotNull(message = "请求端口不能为空")
        private Integer requestPort;

        private String requestPath;
        private String requestHeader;
        private String description;
        private AssetStatusEnum status = AssetStatusEnum.DISABLED;
        private AssetRiskEnum riskLevel = AssetRiskEnum.UNKNOWN;
    }

    /**
     * 资产详情返回值。
     * 区别于分页列表用的 {@link PageAssetVO.Response}，包含完整的资产字段。
     */
    @Data
    public static class Response {
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
}
