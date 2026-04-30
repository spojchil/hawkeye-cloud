package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.business.service.VulTagService;
import com.hawkeye.vul.common.pojo.vo.vul.VulTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "漏洞标签管理")
@RestController
@RequestMapping("/vul-tag")
@RequiredArgsConstructor
public class VulTagController {

    private final VulTagService vulTagService;

    @Operation(summary = "标签列表")
    @GetMapping
    public ApiResponse<List<VulTagVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(vulTagService.list(keyword));
    }
}
