# Task-Service API

> 任务调度核心。Base path 通过 Gateway: `/api/task`
>
> 统一响应格式：`ApiResponse<T>`

## `POST /task` — 创建任务

**返回** `ApiResponse<TaskVO.Response>`

提交后立刻返回，异步执行拆分+投递。

```json
// Request
{ "taskName":"生产环境扫描", "assetIds":[1,2], "templateIds":[1001,1002], "priority":1 }
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskName | String | 是 | 任务名称 |
| assetIds | List\<Long\> | 是 | 资产 ID 列表 |
| templateIds | List\<Long\> | 是 | 模板 ID 列表 |
| priority | Integer | 否 | 优先级，默认 1 |

```json
// Response
{ "data": {
    "taskId": 1, "taskName": "生产环境扫描",
    "status": "执行中", "totalItems": 4,
    "completedItems": 0, "failedItems": 0, "priority": 1,
    "startTime": null, "endTime": null, "resultSummary": null
}}
```

| status 值 | 含义 |
|-----------|------|
| 待执行 | 已入库，等待异步拆分 |
| 执行中 | 拆分完成，已投递 RocketMQ |
| 已完成 | 所有检测项执行完毕 |
| 已取消 | 用户在待执行状态取消 |
| 异常终止 | 拆分阶段失败 |

---

## `GET /task` — 任务列表

**返回** `ApiResponse<ListResult<PageTaskVO.Response>>`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 默认 1 |
| pageSize | int | 否 | 默认 10，最大 100 |
| taskName | String | 否 | 模糊搜索 |
| status | String | 否 | 状态过滤 |

```json
{ "total": 3, "data": [{
    "taskId": 1, "taskName": "生产环境扫描", "status": "执行中",
    "totalItems": 4, "completedItems": 0, "failedItems": 0,
    "priority": 1, "startTime": "2026-04-30T19:15:00", "endTime": null
}]}
```

---

## `GET /task/{taskId}` — 详情

**返回** `ApiResponse<TaskVO.Response>`

```json
// GET /task/1
{ "data": {
    "taskId": 1, "taskName": "生产环境扫描",
    "targetIds": "1,2", "vulIds": "1001,1002",
    "status": "执行中",
    "totalItems": 4, "completedItems": 2, "failedItems": 0,
    "priority": 1,
    "startTime": "2026-04-30T19:15:00", "endTime": null,
    "resultSummary": "{\"matched\":2,\"notMatched\":2,\"error\":0}"
}}
```

| 字段 | 说明 |
|------|------|
| totalItems | M资产 × N模板 |
| startTime | 异步拆分开始时间 |
| endTime | 最后检测项完成时间 |

---

## `DELETE /task/{taskId}` — 取消

**返回** `ApiResponse<Void>`

仅可取消待执行状态的任务，否则返回 2001 错误。

---

---

## 内部流程

```
POST /task
  ↓ 异步线程池
TemplateCache → 拉取模板(Caffeine+Redis+Feign)
AssetServiceFeign → 拉取资产
  ↓ M资产 × N模板
TaskItemMessage(含全量数据) → RocketMQ task_item_topic
  ↓ detection-service 消费
Redis INCR task:{id}:completed
  ↓ TaskProgressScheduler 每2s轮询
completed >= total → task.status = DONE
```

---

## 错误码

| code | 含义 |
|------|------|
| 1002 | 任务不存在 |
| 2001 | 参数校验失败 / 状态不允许 |
| 5001 | 服务器内部错误 |
