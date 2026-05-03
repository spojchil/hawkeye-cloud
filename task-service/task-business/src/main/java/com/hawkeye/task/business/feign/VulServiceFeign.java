package com.hawkeye.task.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "vul-service", path = "/vul")
public interface VulServiceFeign {

    @GetMapping("/{templateId}")
    ApiResponse<VulTemplateVO.Response> getTemplate(@PathVariable Long templateId);
}
