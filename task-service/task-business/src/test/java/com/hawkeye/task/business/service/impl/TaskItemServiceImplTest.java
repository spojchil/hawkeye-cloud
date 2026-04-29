package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.task.business.mapper.TaskItemMapper;
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
        taskItemService = spy(new TaskItemServiceImpl());
        ReflectionTestUtils.setField(taskItemService, "baseMapper", taskItemMapper);

        doReturn(lambdaChain).when(taskItemService).lambdaQuery();
        when(lambdaChain.eq(any(), any())).thenReturn(lambdaChain);
    }

    // === 更新检测结果 updateResult ===

    @Test
    @DisplayName("更新结果 — SUCCESS 状态回写成功")
    void updateResultSuccess() {
        TaskItem item = buildItem(100L, 5001L, 10L, 20L, TaskItemStatusEnum.PENDING, null);
        when(lambdaChain.one()).thenReturn(item);
        when(taskItemMapper.updateById(any(TaskItem.class))).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.SUCCESS);
        request.setResult("{\"matched\":true,\"matcher\":\"word\"}");

        TaskItemVO.Response response = taskItemService.updateResult(100L, request);

        assertAll("回写成功",
                () -> assertEquals(100L, response.getItemId()),
                () -> assertEquals(5001L, response.getTaskId()),
                () -> assertEquals(TaskItemStatusEnum.SUCCESS, response.getStatus()),
                () -> assertEquals("{\"matched\":true,\"matcher\":\"word\"}", response.getResult())
        );
        verify(taskItemMapper).updateById((TaskItem) any(TaskItem.class));
    }

    @Test
    @DisplayName("更新结果 — FAILED 状态回写（网络超时）")
    void updateResultFailed() {
        TaskItem item = buildItem(200L, 5001L, 30L, 40L, TaskItemStatusEnum.PENDING, null);
        when(lambdaChain.one()).thenReturn(item);
        when(taskItemMapper.updateById(any(TaskItem.class))).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.FAILED);
        request.setResult("{\"error\":\"connect timeout\"}");

        TaskItemVO.Response response = taskItemService.updateResult(200L, request);

        assertEquals(TaskItemStatusEnum.FAILED, response.getStatus());
        verify(taskItemMapper).updateById((TaskItem) any(TaskItem.class));
    }

    @Test
    @DisplayName("更新结果 — NO_MATCH 状态回写（检测未命中）")
    void updateResultNoMatch() {
        TaskItem item = buildItem(300L, 5002L, 50L, 60L, TaskItemStatusEnum.PENDING, null);
        when(lambdaChain.one()).thenReturn(item);
        when(taskItemMapper.updateById(any(TaskItem.class))).thenReturn(1);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.NO_MATCH);
        request.setResult("{\"matched\":false}");

        TaskItemVO.Response response = taskItemService.updateResult(300L, request);

        assertEquals(TaskItemStatusEnum.NO_MATCH, response.getStatus());
    }

    @Test
    @DisplayName("更新结果 — item 不存在，抛 ApiException（404）")
    void updateResultNotFound() {
        when(lambdaChain.one()).thenReturn(null);

        TaskItemVO.Request request = new TaskItemVO.Request();
        request.setStatus(TaskItemStatusEnum.SUCCESS);

        ApiException ex = assertThrows(ApiException.class,
                () -> taskItemService.updateResult(999L, request));

        assertAll("detection item 不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("检测项不存在", ex.getMessage())
        );
        verify(taskItemMapper, never()).updateById((TaskItem) any());
    }

    // === helper ===

    private TaskItem buildItem(Long itemId, Long taskId, Long assetId, Long vulId,
                               TaskItemStatusEnum status, String result) {
        TaskItem item = new TaskItem();
        item.setItemId(itemId);
        item.setTaskId(taskId);
        item.setAssetId(assetId);
        item.setVulId(vulId);
        item.setStatus(status);
        item.setResult(result);
        return item;
    }
}
