package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskItemServiceImpl extends ServiceImpl<TaskItemMapper, TaskItem> implements TaskItemService {

    @Override
    @Transactional
    public TaskItemVO.Response updateResult(Long itemId, TaskItemVO.Request request) {
        TaskItem item = lambdaQuery().eq(TaskItem::getItemId, itemId).one();
        if (item == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "检测项不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        item.setStatus(request.getStatus());
        item.setResult(request.getResult());
        updateById(item);

        TaskItemVO.Response response = new TaskItemVO.Response();
        response.setItemId(item.getItemId());
        response.setTaskId(item.getTaskId());
        response.setAssetId(item.getAssetId());
        response.setVulId(item.getVulId());
        response.setStatus(item.getStatus());
        response.setResult(item.getResult());
        return response;
    }
}
