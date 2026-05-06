package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_http_step")
public class VulHttpStep extends BaseVulEntity {

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

    /** Reference to vul_text_content.text_id */
    private Long bodyTextId;

    /** Reference to vul_text_content.text_id (raw HTTP with request line + headers + body) */
    private Long rawTextId;

    /** and / or */
    private String matchersCondition;

    /** batteringram / pitchfork / clusterbomb */
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
