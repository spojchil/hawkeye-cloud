package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;

import java.util.List;

public interface VulCategoryService extends IService<VulCategory> {

    List<VulCategoryVO.Response> tree(Long parentId);

    VulCategoryVO.Response create(VulCategoryVO.Request request);

    VulCategoryVO.Response update(Long categoryId, VulCategoryVO.Request request);

    void delete(Long categoryId);

    int addTemplates(Long categoryId, List<Long> templateIds);

    int removeTemplates(Long categoryId, List<Long> templateIds);
}
