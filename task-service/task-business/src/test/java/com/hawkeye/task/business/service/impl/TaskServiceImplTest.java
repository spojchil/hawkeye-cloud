package com.hawkeye.task.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.task.business.mapstruct.TaskMapstruct;
import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.service.TaskItemService;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.entity.Task;
import com.hawkeye.task.common.pojo.vo.task.PageTaskVO;
import com.hawkeye.task.common.pojo.vo.task.TaskVO;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskServiceImpl 任务服务单元测试")
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskMapstruct taskMapstruct;
    @Mock
    private TaskItemService taskItemService;
    @Mock
    private com.hawkeye.task.business.mapper.TaskItemMapper taskItemMapper;
    @Mock
    private com.hawkeye.task.business.cache.TemplateCache templateCache;
    @Mock
    private com.hawkeye.task.business.feign.AssetServiceFeign assetServiceFeign;
    @Mock
    private com.hawkeye.task.business.mq.TaskProducerService taskProducerService;
    @Mock
    private LambdaQueryChainWrapper<Task> lambdaChain;
    @Mock
    private LambdaUpdateChainWrapper<Task> lambdaUpdateChain;

    private TaskServiceImpl taskService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Task.class);
    }

    @BeforeEach
    void setUp() {
        taskService = spy(new TaskServiceImpl(taskMapstruct, taskItemService, taskItemMapper,
                templateCache, assetServiceFeign, taskProducerService));
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);

        doReturn(lambdaChain).when(taskService).lambdaQuery();
        when(lambdaChain.eq(any(), any())).thenReturn(lambdaChain);
        when(lambdaChain.select((com.baomidou.mybatisplus.core.toolkit.support.SFunction<Task, ?>) any()))
                .thenReturn(lambdaChain);

        doReturn(lambdaUpdateChain).when(taskService).lambdaUpdate();
        when(lambdaUpdateChain.eq(any(), any())).thenReturn(lambdaUpdateChain);
        when(lambdaUpdateChain.set(any(), any())).thenReturn(lambdaUpdateChain);
        when(lambdaUpdateChain.update()).thenReturn(true);

        when(taskMapstruct.toEntity(any(TaskVO.Request.class))).thenAnswer(inv -> {
            TaskVO.Request req = inv.getArgument(0);
            Task task = new Task();
            task.setTaskName(req.getTaskName());
            task.setTargetIds(req.getAssetIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
            task.setVulIds(req.getTemplateIds().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
            task.setPriority(req.getPriority() != null ? req.getPriority() : 1);
            return task;
        });

        when(taskMapstruct.toResponseVO(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            TaskVO.Response resp = new TaskVO.Response();
            resp.setTaskId(t.getTaskId());
            resp.setTaskName(t.getTaskName());
            resp.setTargetIds(t.getTargetIds());
            resp.setVulIds(t.getVulIds());
            resp.setStatus(t.getStatus());
            resp.setTotalItems(t.getTotalItems());
            resp.setCompletedItems(t.getCompletedItems());
            resp.setFailedItems(t.getFailedItems());
            resp.setPriority(t.getPriority());
            resp.setStartTime(t.getStartTime());
            resp.setEndTime(t.getEndTime());
            resp.setResultSummary(t.getResultSummary());
            return resp;
        });

        when(taskMapstruct.toPageTaskVO(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            PageTaskVO.Response resp = new PageTaskVO.Response();
            resp.setTaskId(t.getTaskId());
            resp.setTaskName(t.getTaskName());
            resp.setStatus(t.getStatus());
            resp.setTotalItems(t.getTotalItems());
            resp.setCompletedItems(t.getCompletedItems());
            resp.setFailedItems(t.getFailedItems());
            resp.setPriority(t.getPriority());
            resp.setStartTime(t.getStartTime());
            resp.setEndTime(t.getEndTime());
            return resp;
        });
    }

    // === 创建任务 create ===

    @Test
    @DisplayName("创建任务 — 正常流程，status=PENDING，计数初始化为 0")
    void createSuccess() {
        TaskVO.Request request = new TaskVO.Request();
        request.setTaskName("常规扫描");
        request.setAssetIds(List.of(1L, 2L, 3L));
        request.setTemplateIds(List.of(10L, 20L, 30L));
        request.setPriority(1);

        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        TaskVO.Response response = taskService.create(request);

        assertAll("创建任务成功",
                () -> assertEquals("常规扫描", response.getTaskName()),
                () -> assertEquals("1,2,3", response.getTargetIds()),
                () -> assertEquals("10,20,30", response.getVulIds()),
                () -> assertEquals(TaskStatusEnum.PENDING, response.getStatus()),
                () -> assertEquals(0, response.getTotalItems()),
                () -> assertEquals(0, response.getCompletedItems()),
                () -> assertEquals(0, response.getFailedItems()),
                () -> assertEquals(1, response.getPriority())
        );
        verify(taskMapper).insert(any(Task.class));
    }

    @Test
    @DisplayName("创建任务 — priority 为 null 时默认设为 1")
    void createDefaultPriority() {
        TaskVO.Request request = new TaskVO.Request();
        request.setTaskName("test");
        request.setAssetIds(List.of(1L));
        request.setTemplateIds(List.of(1L));

        when(taskMapper.insert(any(Task.class))).thenReturn(1);

        TaskVO.Response response = taskService.create(request);

        assertEquals(1, response.getPriority());
    }

    // === 查询任务详情 getById ===

    @Test
    @DisplayName("查询任务详情 — 任务存在，返回完整信息")
    void getByIdFound() {
        Task task = buildTask(5001L, "扫描任务", TaskStatusEnum.RUNNING, 100, 42, 3);
        when(taskMapper.selectById(5001L)).thenReturn(task);

        TaskVO.Response response = taskService.getById(5001L);

        assertAll("查询成功",
                () -> assertEquals(5001L, response.getTaskId()),
                () -> assertEquals("扫描任务", response.getTaskName()),
                () -> assertEquals(TaskStatusEnum.RUNNING, response.getStatus()),
                () -> assertEquals(100, response.getTotalItems()),
                () -> assertEquals(42, response.getCompletedItems()),
                () -> assertEquals(3, response.getFailedItems())
        );
    }

    @Test
    @DisplayName("查询任务详情 — 任务不存在，抛 ApiException（404）")
    void getByIdNotFound() {
        when(taskMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> taskService.getById(999L));

        assertAll("任务不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("任务不存在", ex.getMessage())
        );
    }

    // === 分页查询 pageQuery ===

    @Test
    @DisplayName("基本分页查询 — 无任何筛选条件，默认 page=1 pageSize=10")
    void pageQueryDefaults() {
        PageTaskVO.Request request = new PageTaskVO.Request();
        List<Task> tasks = List.of(
                buildTask(1L, "任务A", TaskStatusEnum.DONE, 50, 50, 0),
                buildTask(2L, "任务B", TaskStatusEnum.RUNNING, 200, 100, 5)
        );
        stubSelectPage(tasks);

        ListResult<PageTaskVO.Response> result = taskService.pageQuery(request);

        assertAll("默认分页",
                () -> assertEquals(2, result.getTotal()),
                () -> assertEquals(2, result.getData().size()),
                () -> assertEquals("任务A", result.getData().get(0).getTaskName()),
                () -> assertEquals(TaskStatusEnum.DONE, result.getData().get(0).getStatus()),
                () -> assertEquals("任务B", result.getData().get(1).getTaskName())
        );
    }

    @Test
    @DisplayName("分页查询 — 按任务名称模糊筛选")
    void pageQueryWithNameFilter() {
        PageTaskVO.Request request = new PageTaskVO.Request();
        request.setTaskName("扫描");
        Task task = buildTask(1L, "11月扫描", TaskStatusEnum.DONE, 10, 10, 0);
        stubSelectPage(List.of(task));

        ListResult<PageTaskVO.Response> result = taskService.pageQuery(request);

        assertAll("按名称筛选",
                () -> assertEquals(1, result.getTotal()),
                () -> assertEquals("11月扫描", result.getData().get(0).getTaskName())
        );
    }

    @Test
    @DisplayName("分页查询 — 按状态筛选")
    void pageQueryWithStatusFilter() {
        PageTaskVO.Request request = new PageTaskVO.Request();
        request.setStatus(TaskStatusEnum.RUNNING);
        Task task = buildTask(1L, "running-task", TaskStatusEnum.RUNNING, 30, 15, 2);
        stubSelectPage(List.of(task));

        ListResult<PageTaskVO.Response> result = taskService.pageQuery(request);

        assertAll("按状态筛选",
                () -> assertEquals(1, result.getTotal()),
                () -> assertEquals(TaskStatusEnum.RUNNING, result.getData().get(0).getStatus())
        );
    }

    @Test
    @DisplayName("分页查询 — 无数据，返回空列表")
    void pageQueryEmpty() {
        PageTaskVO.Request request = new PageTaskVO.Request();
        stubSelectPage(Collections.emptyList());

        ListResult<PageTaskVO.Response> result = taskService.pageQuery(request);

        assertAll("空结果",
                () -> assertEquals(0, result.getTotal()),
                () -> assertTrue(result.getData().isEmpty())
        );
    }

    @Test
    @DisplayName("分页查询 — pageSize 超过上限 100 时自动截断")
    void pageQuerySizeLimit() {
        PageTaskVO.Request request = new PageTaskVO.Request();
        request.setPage(1);
        request.setPageSize(9999);
        stubSelectPage(Collections.emptyList());

        taskService.pageQuery(request);
        verify(taskMapper).selectPage(argThat(page -> {
            if (page instanceof com.baomidou.mybatisplus.extension.plugins.pagination.Page<?> mpPage) {
                return mpPage.getSize() == 100;
            }
            return false;
        }), any());
    }

    // === 取消任务 cancel ===

    @Test
    @DisplayName("取消任务 — PENDING 状态可取消，原子 UPDATE 成功")
    void cancelSuccess() {
        when(lambdaUpdateChain.update()).thenReturn(true);

        taskService.cancel(1L);

        verify(lambdaUpdateChain).eq(any(), eq(1L));
        verify(lambdaUpdateChain).eq(any(), eq(TaskStatusEnum.PENDING));
        verify(lambdaUpdateChain).set(any(), eq(TaskStatusEnum.CANCELLED));
        verify(lambdaUpdateChain).update();
        verify(taskMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("取消任务 — 任务不存在，UPDATE 返回 0，selectById 返回 null，抛 404")
    void cancelNotFound() {
        when(lambdaUpdateChain.update()).thenReturn(false);
        when(taskMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> taskService.cancel(999L));

        assertAll("任务不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("任务不存在", ex.getMessage())
        );
    }

    @Test
    @DisplayName("取消任务 — RUNNING 状态，UPDATE 返回 0，selectById 返回 RUNNING 任务，抛 400")
    void cancelNotPending() {
        when(lambdaUpdateChain.update()).thenReturn(false);
        Task task = buildTask(1L, "运行中", TaskStatusEnum.RUNNING, 100, 42, 3);
        when(taskMapper.selectById(1L)).thenReturn(task);

        ApiException ex = assertThrows(ApiException.class,
                () -> taskService.cancel(1L));

        assertAll("非 PENDING 不可取消",
                () -> assertEquals(CommonErrorCode.PARAM_INVALID.getCode(), ex.getCode()),
                () -> assertEquals("仅待执行状态的任务可取消", ex.getMessage())
        );
    }

    // === 查询运行中任务 ID listRunningTaskIds ===

    @Test
    @DisplayName("查询运行中任务 — 有 2 个 RUNNING 任务")
    void listRunningTaskIds() {
        Task t1 = buildTask(1L, "t1", TaskStatusEnum.RUNNING, 50, 25, 0);
        Task t2 = buildTask(2L, "t2", TaskStatusEnum.RUNNING, 30, 15, 1);
        when(lambdaChain.list()).thenReturn(List.of(t1, t2));

        List<Long> ids = taskService.listRunningTaskIds();

        assertAll("运行中任务ID",
                () -> assertEquals(2, ids.size()),
                () -> assertEquals(1L, ids.get(0)),
                () -> assertEquals(2L, ids.get(1))
        );
    }

    @Test
    @DisplayName("查询运行中任务 — 无 RUNNING 任务，返回空列表")
    void listRunningTaskIdsEmpty() {
        when(lambdaChain.list()).thenReturn(Collections.emptyList());

        List<Long> ids = taskService.listRunningTaskIds();

        assertTrue(ids.isEmpty());
    }

    // === helper methods ===

    private Task buildTask(Long id, String name, TaskStatusEnum status,
                           int total, int completed, int failed) {
        Task task = new Task();
        task.setTaskId(id);
        task.setTaskName(name);
        task.setTargetIds("1,2,3");
        task.setVulIds("10,20");
        task.setStatus(status);
        task.setTotalItems(total);
        task.setCompletedItems(completed);
        task.setFailedItems(failed);
        task.setPriority(1);
        return task;
    }

    @SuppressWarnings("unchecked")
    private void stubSelectPage(List<Task> tasks) {
        when(taskMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Task> page = inv.getArgument(0);
            page.setRecords(tasks);
            page.setTotal(tasks.size());
            return page;
        });
    }
}
