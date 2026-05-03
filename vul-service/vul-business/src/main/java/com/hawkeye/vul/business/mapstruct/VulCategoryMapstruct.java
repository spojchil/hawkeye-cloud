package com.hawkeye.vul.business.mapstruct;

import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VulCategoryMapstruct {

    VulCategory toEntity(VulCategoryVO.Request request);

    VulCategoryVO.Response toResponseVO(VulCategory entity);
}
