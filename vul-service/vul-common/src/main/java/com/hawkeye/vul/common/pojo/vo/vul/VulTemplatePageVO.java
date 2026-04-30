package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 模板列表项（不含大字段）。 */
@Data
public class VulTemplatePageVO {
    private Long id;
    private String templateId;
    private String name;
    private String severity;
    private String cveId;
    private BigDecimal cvssScore;
    private List<String> tags;
    private List<String> categories;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createTime;
}
