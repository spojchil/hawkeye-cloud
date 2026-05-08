package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提取器配置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_extractor")
public class VulExtractor extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long extractorId;

    private Long templateId;

    /** NULL=global, non-NULL=step-specific */
    private Integer stepOrder;

    /** regex / json / kval / dsl / xpath */
    private String type;

    /** body / header */
    private String part;

    /** 提取变量名，写入 VariableContext */
    private String extractorName;

    /** JSON: type-specific config */
    private String config;

    private Boolean internal;

    private Integer groupNum;
}
