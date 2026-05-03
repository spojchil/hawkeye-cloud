package com.hawkeye.vul.common.pojo.vo.vul;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VulTemplateImportVO {

    /** Nuclei template JSON (frontend-deserialized). */
    @NotEmpty(message = "模板数据不能为空")
    private Map<String, Object> template;

    /** 关联分类 ID 列表（可选）. */
    private List<Long> categoryIds;
}
