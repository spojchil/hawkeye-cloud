package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 匹配器配置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_matcher")
public class VulMatcher extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long matcherId;

    private Long templateId;

    /** NULL=global, non-NULL=step-specific */
    private Integer stepOrder;

    /** word / status / dsl / regex / size / xpath / binary */
    private String type;

    /** body / header / all */
    private String part;

    /** and / or — 因 condition 是 MySQL 保留字，改名为 innerCondition */
    private String innerCondition;

    private Boolean negative;

    private Boolean caseInsensitive;

    private Boolean internal;

    private Boolean matchAll;

    private String matcherName;

    /** JSON: type-specific config (words[] / dsl[] / regex[] etc.) */
    private String config;
}
