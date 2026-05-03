package com.hawkeye.vul.business.mapstruct;

import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VulTemplateMapstruct {

    VulTemplatePageVO.Response toPageVO(VulTemplate entity);

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "httpSteps", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "variables", ignore = true)
    VulTemplateVO.Response toResponseVO(VulTemplate entity);
}
