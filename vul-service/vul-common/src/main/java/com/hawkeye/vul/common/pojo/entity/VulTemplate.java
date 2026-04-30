package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 漏洞模板主表（v2）。
 * 只存元数据和 1:1 分类信息，检测逻辑拆到了 vul_http_step / vul_matcher / vul_extractor。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template")
public class VulTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** YAML id 小写，业务唯一键 */
    private String templateId;

    private String name;

    private String description;

    private String author;

    /** critical / high / medium / low / info / unknown */
    private String severity;

    private String cveId;

    /** 可复合，如 "CWE-20,CWE-77" */
    private String cweId;

    private BigDecimal cvssScore;

    private BigDecimal epssScore;

    /** 多步骤执行流，如 http(1) && http(2) */
    private String flow;

    /** 模板级变量，MySQL JSON → String */
    private String variables;

    private Boolean enabled;

    private Integer version;
}
