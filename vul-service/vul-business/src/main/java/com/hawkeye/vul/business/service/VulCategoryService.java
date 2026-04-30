package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryRequestVO;

import java.util.List;

public interface VulCategoryService extends IService<VulCategory> {

    List<VulCategoryVO> tree(Long parentId);

    Long create(VulCategoryRequestVO request);

    void update(Long id, VulCategoryRequestVO request);

    void delete(Long id);

    int addTemplates(Long categoryId, List<Long> templateIds);

    int removeTemplates(Long categoryId, List<Long> templateIds);
}
