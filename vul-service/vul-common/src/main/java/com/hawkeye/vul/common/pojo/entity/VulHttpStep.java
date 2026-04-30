package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * HTTP 请求步骤。
 * 每个模板可有 1~N 步，step_order 从 1 开始。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_http_step")
public class VulHttpStep extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Integer stepOrder;

    private String method;

    /** JSON 数组，如 ["{{BaseURL}}/admin"] */
    private String path;

    /** JSON 键值对 */
    private String headers;

    private String body;

    /** 原始 HTTP 文本（raw 模式） */
    private String raw;

    /** 爆破模式：batteringram / pitchfork / clusterbomb */
    private String attack;

    /** 本步骤内 matcher 间关系：and / or */
    private String matchersCondition;

    private Boolean stopAtFirstMatch;
}
