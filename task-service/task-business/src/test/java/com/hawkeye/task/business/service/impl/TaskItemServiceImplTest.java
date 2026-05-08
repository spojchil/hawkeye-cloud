package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.task.business.mapper.TaskItemMapper;
import com.hawkeye.task.business.mapstruct.TaskItemMapstruct;
import com.hawkeye.task.common.enums.TaskItemStatusEnum;
import com.hawkeye.task.common.pojo.entity.TaskItem;
import com.hawkeye.task.common.pojo.vo.task.TaskItemVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskItemServiceImpl 检测项服务单元测试")
class TaskItemServiceImplTest {

    @Mock
    private TaskItemMapper taskItemMapper;
    @Mock
    private TaskItemMapstruct taskItemMapstruct;
    @Mock
    private LambdaQueryChainWrapper<TaskItem> lambdaChain;

    private TaskItemServiceImpl taskItemService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, TaskItem.class);
    }

    @BeforeEach
    void setUp() {
        taskItemService = spy(new TaskItemServiceImpl(taskItemMapstruct));
        ReflectionTestUtils.setField(taskItemService, "baseMapper", taskItemMapper);

        doReturn(lambdaChain).when(taskItemService).lambdaQuery();
        when(lambdaChain.eq(any(), any())).thenReturn(lambdaChain);

        when(taskItemMapstruct.toResponseVO(any(TaskItem.class))).thenAnswer(inv -> {
            TaskItem item = inv.getArgument(0);
            TaskItemVO.Response resp = new TaskItemVO.Response();
            resp.setItemId(item.getItemId());
            resp.setTaskId(item.getTaskId());
            resp.setAssetId(item.getAssetId());
            resp.setVulId(item.getVulId());
            resp.setStatus(item.getStatus());
            return resp;
        });
    }

    // === 更新检测结果 updateResult ===

    @Test
    @DisplayName("更新结果 — MATCHED 状态回写成功")
    void updateResultSuccess() {
        TaskItem item = buildItem(100L, 5001L, 10L, 20L);
        when(taskItemMapper.selectById(100L)).thenReturn(item);
        when(taskItemMapper.updateById((TaskItem) any())).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.MATCHED);

        TaskItemVO.Response response = taskItemService.updateResult(100L, request);

        assertAll("回写成功",
                () -> assertEquals(100L, response.getItemId()),
                () -> assertEquals(5001L, response.getTaskId()),
                () -> assertEquals(TaskItemStatusEnum.MATCHED, response.getStatus())
        );
        verify(taskItemMapper).updateById((TaskItem) any());
    }

    @Test
    @DisplayName("更新结果 — FAILED 状态回写（网络超时）")
    void updateResultFailed() {
        TaskItem item = buildItem(200L, 5001L, 30L, 40L);
        when(taskItemMapper.selectById(200L)).thenReturn(item);
        when(taskItemMapper.updateById((TaskItem) any())).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.FAILED);

        TaskItemVO.Response response = taskItemService.updateResult(200L, request);

        assertEquals(TaskItemStatusEnum.FAILED, response.getStatus());
        verify(taskItemMapper).updateById((TaskItem) any());
    }

    @Test
    @DisplayName("更新结果 — NOT_MATCHED 状态回写（检测未命中）")
    void updateResultNoMatch() {
        TaskItem item = buildItem(300L, 5002L, 50L, 60L);
        when(taskItemMapper.selectById(300L)).thenReturn(item);
        when(taskItemMapper.updateById((TaskItem) any())).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.NOT_MATCHED);
        request.setResult("{\"matched\":false}");

        TaskItemVO.Response response = taskItemService.updateResult(300L, request);

        assertEquals(TaskItemStatusEnum.NOT_MATCHED, response.getStatus());
    }

    @Test
    @DisplayName("更新结果 — item 不存在，抛 ApiException（404）")
    void updateResultNotFound() {
        when(taskItemMapper.selectById(anyLong())).thenReturn(null);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.MATCHED);

        ApiException ex = assertThrows(ApiException.class,
                () -> taskItemService.updateResult(999L, request));

        assertAll("detection item 不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("检测项不存在", ex.getMessage())
        );
        verify(taskItemMapper, never()).updateById((TaskItem) any());
    }

    // === helper ===

    private TaskItem buildItem(Long itemId, Long taskId, Long assetId, Long vulId) {
        TaskItem item = new TaskItem();
        item.setItemId(itemId);
        item.setTaskId(taskId);
        item.setAssetId(assetId);
        item.setVulId(vulId);
        item.setStatus(TaskItemStatusEnum.PENDING);
        return item;
    }
}
