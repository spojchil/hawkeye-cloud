package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

/** 模板分页查询参数。 */
@Data
public class VulTemplatePageQueryVO {
    private Integer page = 1;
    private Integer size = 20;
    private String name;
    private String severity;
    private String tag;
    private Long categoryId;
    private Boolean enabled;
    private String sort = "create_time_desc";
}
