package com.hawkeye.gateway.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT Token模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {
    /**
     * Token字符串
     */
    private String token;
}
