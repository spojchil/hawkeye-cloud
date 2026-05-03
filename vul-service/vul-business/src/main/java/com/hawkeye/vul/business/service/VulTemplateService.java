package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.vo.vul.NucleiTemplateVO;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;

public interface VulTemplateService extends IService<VulTemplate> {

    ListResult<VulTemplatePageVO.Response> pageQuery(VulTemplatePageVO.Request request);

    VulTemplateVO.Response getDetail(Long templateId);

    void delete(Long templateId);

    VulTemplateVO.Response importTemplate(NucleiTemplateVO template, java.util.List<Long> categoryIds);

    VulTemplateDetectDTO getForDetection(Long templateId);

    VulTemplate getByYamlId(String yamlId);
}
