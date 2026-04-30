package com.hawkeye.vul.common.pojo.vo.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 分类树节点。 */
@Data
public class VulCategoryVO {
    private Long categoryId;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String description;
    private Long templateCount;
    private List<VulCategoryVO> children;
}
