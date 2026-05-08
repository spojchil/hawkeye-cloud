package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文本内容
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_text_content")
public class VulTextContent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long textId;

    private String content;
}
