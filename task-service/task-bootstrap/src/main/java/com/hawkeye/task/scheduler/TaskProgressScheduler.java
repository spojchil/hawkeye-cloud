package com.hawkeye.task.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hawkeye.task.business.mapper.TaskMapper;
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
 * ★ 内存缓存上次的 completed 值，只在有变化时才写 DB，减少无谓 UPDATE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProgressScheduler {

    private final TaskMapper taskMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COMPLETED_KEY_PREFIX = "task:";
    private static final String COMPLETED_KEY_SUFFIX = ":completed";

    private final ConcurrentHashMap<Long, Integer> lastCompletedMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${task.progress.poll-interval-ms:2000}")
    public void pollProgress() {
        List<Long> runningIds = taskMapper.selectList(
                        new LambdaQueryWrapper<Task>()
                                .eq(Task::getStatus, TaskStatusEnum.RUNNING)
                                .select(Task::getTaskId))
                .stream()
                .map(Task::getTaskId)
                .toList();

        if (runningIds.isEmpty()) {
            return;
        }

        for (Long taskId : runningIds) {
            try {
                String key = COMPLETED_KEY_PREFIX + taskId + COMPLETED_KEY_SUFFIX;
                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) {
                    continue;
                }
                int completed = Integer.parseInt(value.toString());

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
}
