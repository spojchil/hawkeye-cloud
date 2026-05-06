package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_extractor")
public class VulExtractor extends BaseVulEntity {

    @TableId(type = IdType.AUTO)
    private Long extractorId;

    private Long templateId;

    /** NULL=global; non-NULL=step-specific */
    private Integer stepOrder;

    /** regex / json / kval / dsl / xpath */
    private String type;

    /** body / header */
    private String part;

    /** Variable name written to VariableContext */
    private String extractorName;

    /** JSON: type-specific config (regex[] / json[] / kval[] / dsl[] etc.) */
    private String config;

    private Boolean internal;

    private Integer groupNum;
}
