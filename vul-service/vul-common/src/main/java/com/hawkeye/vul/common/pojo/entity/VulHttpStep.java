package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * HTTP 探测步骤
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_http_step")
public class VulHttpStep extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long httpId;

    private Long templateId;

    private Integer stepOrder;

    private String httpName;

    private String method;

    /** JSON array, e.g. ["{{BaseURL}}/admin"] */
    private String path;

    /** JSON key-value pairs */
    private String headers;

    private Long bodyTextId;

    private Long rawTextId;

    /** and / or */
    private String matchersCondition;

    private String attack;

    /** JSON: payload definitions */
    private String payloads;

    private Boolean stopAtFirstMatch;

    private Boolean selfContained;

    private Boolean redirects;

    private Integer maxRedirects;

    private Boolean hostRedirects;

    private Boolean unsafe;

    private Boolean cookieReuse;

    private Boolean reqCondition;
}
