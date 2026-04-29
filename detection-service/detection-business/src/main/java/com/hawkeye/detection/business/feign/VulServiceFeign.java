package com.hawkeye.detection.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * vul-service 内部 Feign 客户端。
 */
@FeignClient(name = "vul-service", path = "/vul")
public interface VulServiceFeign {

    @GetMapping("/internal/{id}")
    ApiResponse<VulTemplateDTO> getTemplate(@PathVariable("id") Long id);
}
