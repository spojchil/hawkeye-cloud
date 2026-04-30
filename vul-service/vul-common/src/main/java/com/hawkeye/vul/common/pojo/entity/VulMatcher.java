package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 匹配器。
 * step_order = NULL 表示全局 matcher，非 NULL 表示该步骤专属。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_matcher")
public class VulMatcher extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** NULL=全局；非NULL=步骤专属 */
    private Integer stepOrder;

    /** word / status / dsl / regex / size / xpath / binary */
    private String type;

    /** body / header / all */
    private String part;

    /** and / or */
    /** and / or。列名是 MySQL 保留字，需转义 */
    @TableField("`condition`")
    private String condition;

    private Boolean negative;

    private Boolean caseInsensitive;

    private Boolean internal;

    private String name;

    /** JSON：类型特定配置（words[] / status[] / dsl[] 等） */
    private String config;
}
