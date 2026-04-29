package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.PageVulVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulVO;

public interface VulTemplateService extends IService<VulTemplate> {

    ListResult<PageVulVO.Response> pageQuery(PageVulVO.Request request);

    VulVO.Response getById(Long id);

    VulVO.Response create(VulVO.Request request);

    VulVO.Response update(Long id, VulVO.Request request);

    void delete(Long id);

    /**
     * 给 detection-service 使用的接口，仅返回检测执行所需的字段。
     */
    VulTemplateDetectDTO getForDetection(Long id);

    /**
     * 按 YAML templateId 查询，用于导入去重。
     */
    VulTemplate getByTemplateId(String templateId);
}
