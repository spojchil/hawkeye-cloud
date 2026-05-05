package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapstruct.TaskItemMapstruct;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检测项服务实现。
 * <p>
 * 注：检测结果现在直接存储在 task_item 表中，由 detection-service 直接更新。
 * 此服务主要用于查询和管理检测项。
 */
@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl extends ServiceImpl<TaskItemMapper, TaskItem> implements TaskItemService {

    private final TaskItemMapstruct taskItemMapstruct;

    @Override
    @Transactional
    public TaskItemVO.Response updateResult(Long itemId, TaskItemVO.Request request) {
        TaskItem item = baseMapper.selectById(itemId);
        if (item == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "检测项不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        item.setStatus(request.getStatus());
        // 注意：检测结果现在由 detection-service 直接更新到 task_item 表的各个字段
        // 此方法仅更新状态
        updateById(item);
        return taskItemMapstruct.toResponseVO(item);
    }
}
