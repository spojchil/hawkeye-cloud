package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/vul/internal")
@RequiredArgsConstructor
public class VulInternalController {

    private final VulTemplateService vulTemplateService;

    @GetMapping("/{templateId}")
    public ApiResponse<VulTemplateDetectDTO> getForDetection(@PathVariable Long templateId) {
        return ApiResponse.success(vulTemplateService.getForDetection(templateId));
    }

    @PostMapping("/batch")
    public ApiResponse<List<VulTemplateDetectDTO>> batchGetForDetection(
            @RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.success(List.of());
        }
        List<VulTemplateDetectDTO> result = ids.stream()
                .map(vulTemplateService::getForDetection)
                .toList();
        return ApiResponse.success(result);
    }
}
