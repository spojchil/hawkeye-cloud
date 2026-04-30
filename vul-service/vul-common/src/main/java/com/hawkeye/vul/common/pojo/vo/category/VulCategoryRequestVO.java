package com.hawkeye.vul.common.pojo.vo.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VulCategoryRequestVO {
    @NotBlank
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String description;
}
