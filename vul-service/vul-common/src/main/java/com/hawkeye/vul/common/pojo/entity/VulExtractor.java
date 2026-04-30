package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提取器。
 * step_order = NULL 表示全局 extractor。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_extractor")
public class VulExtractor extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** NULL=全局；非NULL=步骤专属 */
    private Integer stepOrder;

    /** regex / json / kval / dsl */
    private String type;

    /** body / header */
    private String part;

    /** 提取变量名 */
    private String name;

    /** JSON：类型特定配置 */
    private String config;

    private Boolean internal;

    private Integer groupNum;
}
