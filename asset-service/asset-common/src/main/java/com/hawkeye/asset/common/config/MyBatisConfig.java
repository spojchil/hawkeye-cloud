package com.hawkeye.asset.common.config;

import com.common.utils.enums.CodeEnumTypeHandler;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {
    @Bean
    public CodeEnumTypeHandler<RequestMethodEnum> requestMethodHandler() {
        return new CodeEnumTypeHandler<>(RequestMethodEnum.class);
    }
    @Bean
    public CodeEnumTypeHandler<AssetStatusEnum> assetStatusHandler() {
        return new CodeEnumTypeHandler<>(AssetStatusEnum.class);
    }
    @Bean
    public CodeEnumTypeHandler<AssetRiskEnum> assetRiskHandler() {
        return new CodeEnumTypeHandler<>(AssetRiskEnum.class);
    }
}
