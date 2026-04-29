package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 漏洞检测模板主表。
 * <p>
 * 模板数据来源于 Nuclei YAML（~10,688 个），仅保留 http 协议模板。
 * templateId 是 YAML 的 id 字段（如 "CVE-2024-0012"），作为业务唯一键。
 * httpRequests / matchers / extractors 三个大 JSON 字段默认不参与 SELECT，
 * 列表查询时不会加载，仅在详情接口中单独取出。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template")
public class VulTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateId;

    private String name;

    private String description;

    private String author;

    private String severity;

    private String tags;

    private String reference;

    private String classification;

    private String metadata;

    private String flow;

    private String variables;

    private String httpRequests;

    private String matchers;

    private String extractors;

    private Boolean enabled;

    private Integer version;
}
