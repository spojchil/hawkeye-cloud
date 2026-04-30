package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateDetailVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageQueryVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateRequestVO;

import java.util.List;

public interface VulTemplateService extends IService<VulTemplate> {

    IPage<VulTemplatePageVO> pageQuery(VulTemplatePageQueryVO query);

    VulTemplateDetailVO getDetail(Long id);

    Long create(VulTemplateRequestVO request);

    void update(Long id, VulTemplateRequestVO request);

    void delete(Long id);

    void batchDelete(List<Long> ids);

    void setEnabled(Long id, Boolean enabled);

    /** 给 detection-service 的 Feign 接口，返回检测执行所需全部配置。 */
    VulTemplateDetectDTO getForDetection(Long id);

    VulTemplate getByTemplateId(String templateId);
}
