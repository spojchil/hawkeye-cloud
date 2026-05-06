package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_category")
public class VulCategory extends BaseVulEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private String description;
}
