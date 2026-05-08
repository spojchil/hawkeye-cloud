package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 漏洞模板
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template")
public class VulTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long templateId;

    /** YAML id (lowercase), NOT NULL per DDL uk_tenant_yaml_deleted */
    private String yamlId;

    private String name;

    private String author;

    private String description;

    private String impact;

    /** critical / high / medium / low / info / unknown */
    private String severity;

    /** JSON: custom metadata, free key-value */
    private String metadata;

    private String cveId;

    /** Composite CWE, e.g. "CWE-20,CWE-77" */
    private String cweId;

    /** CVSS vector, e.g. "3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H" */
    private String cvssMetrics;

    private Double cvssScore;

    private Double epssScore;

    /** CPE identifier, e.g. "cpe:/a:vendor:product:version" */
    private String cpe;

    private String remediation;

    /** Multi-step flow expression, e.g. "http(1) && http(2)" */
    private String flow;

    /** JSON: template-level dynamic variables */
    private String variables;

    private Boolean enabled;
}
