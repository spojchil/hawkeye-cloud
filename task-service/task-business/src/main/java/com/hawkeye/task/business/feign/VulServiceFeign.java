package com.hawkeye.task.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * vul-service 内部 Feign — 获取模板检测配置。
 */
@FeignClient(name = "vul-service", path = "/vul/internal")
public interface VulServiceFeign {

    @GetMapping("/{id}")
    ApiResponse<VulTemplateDetectDTO> getTemplate(@PathVariable("id") Long id);

    @PostMapping("/batch")
    ApiResponse<List<VulTemplateDetectDTO>> batchGetTemplates(@RequestBody Map<String, List<Long>> body);
}
