package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_matcher")
public class VulMatcher extends BaseVulEntity {

    @TableId(type = IdType.AUTO)
    private Long matcherId;

    private Long templateId;

    /** NULL=global; non-NULL=step-specific */
    private Integer stepOrder;

    /** word / status / dsl / regex / size / xpath / binary */
    private String type;

    /** body / header / all / content_type / location */
    private String part;

    /** and / or — renamed from 'condition' (MySQL reserved word) */
    private String innerCondition;

    private Boolean negative;

    private Boolean caseInsensitive;

    private Boolean internal;

    private Boolean matchAll;

    private String matcherName;

    /** JSON: type-specific config (words[] / status[] / dsl[] / regex[] etc.) */
    private String config;
}
