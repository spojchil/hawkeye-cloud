package com.hawkeye.vul.common.pojo.vo.vul;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VulTemplateImportVO {

    @NotNull(message = "模板数据不能为空")
    @Valid
    private NucleiTemplateVO template;

    private List<Long> categoryIds;
}
