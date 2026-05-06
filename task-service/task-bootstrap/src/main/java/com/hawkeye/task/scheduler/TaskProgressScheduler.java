package com.hawkeye.task.scheduler;

import com.hawkeye.task.business.mapper.TaskMapper;
import com.hawkeye.task.business.service.TaskService;
import com.hawkeye.task.common.enums.TaskStatusEnum;
import com.hawkeye.task.common.pojo.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务进度轮询调度器。
 * <p>
 * 每 2s 轮询 Redis 中 detection-service 写入的完成计数，更新 task 表进度。
 * completed >= totalItems 时标记任务为 DONE。
 * <p>
 * Redis 键格式：
 * - task:{taskId}:matched
 * - task:{taskId}:not_matched
 * - task:{taskId}:error
 * <p>
 * ★ 内存缓存上次的 completed 值，只在有变化时才写 DB，减少无谓 UPDATE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProgressScheduler {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "task:";
    /** 检测完成的状态列表 */
    private static final List<String> STATUS_KEYS = List.of("matched", "not_matched", "error");

    private final ConcurrentHashMap<Long, Integer> lastCompletedMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${task.progress.poll-interval-ms:2000}")
    public void pollProgress() {
        List<Long> runningIds = taskService.listRunningTaskIds();

        if (runningIds.isEmpty()) {
            return;
        }

        for (Long taskId : runningIds) {
            try {
                // 查询所有完成状态的计数并汇总
                int completed = getCompletedCount(taskId);

                Integer last = lastCompletedMap.put(taskId, completed);
                if (last != null && last == completed) {
                    continue;
                }

                Task task = taskMapper.selectById(taskId);
                if (task == null) {
                    lastCompletedMap.remove(taskId);
                    continue;
                }

                task.setCompletedItems(completed);
                taskMapper.updateById(task);

                if (task.getTotalItems() != null && completed >= task.getTotalItems()) {
                    task.setStatus(TaskStatusEnum.DONE);
                    task.setEndTime(LocalDateTime.now());
                    taskMapper.updateById(task);
                    lastCompletedMap.remove(taskId);
                    log.info("任务完成: taskId={}, completed={}/{}, status=DONE",
                            taskId, completed, task.getTotalItems());
                }
            } catch (Exception e) {
                log.error("轮询任务进度异常: taskId={}", taskId, e);
            }
        }
    }

    /**
     * 获取任务的完成计数（汇总所有状态）。
     */
    private int getCompletedCount(Long taskId) {
        int total = 0;
        for (String status : STATUS_KEYS) {
            String key = KEY_PREFIX + taskId + ":" + status;
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    total += Integer.parseInt(value);
                }
            } catch (Exception e) {
                log.warn("读取 Redis 计数失败: key={}", key, e);
            }
        }
        return total;
    }
}
