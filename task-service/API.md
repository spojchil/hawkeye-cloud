# Task-Service API

> 任务调度核心。Base path 通过 Gateway: `/api/task`

## `POST /task` — 创建任务

提交后立刻返回 taskId，异步执行：拉取模板配置 + 资产信息 → M×N 拆分 → 投递 RocketMQ。

```json
// Request
{
  "taskName": "生产环境 CVE 扫描",
  "assetIds": [1, 2],
  "templateIds": [1001, 1002],
  "priority": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskName | String | 是 | 任务名称 |
| assetIds | List\<Long\> | 是 | 资产 ID 列表 |
| templateIds | List\<Long\> | 是 | 模板 ID 列表 |
| priority | Integer | 否 | 优先级，默认 1 |

```json
// Response
{
  "data": {
    "taskId": 1,
    "taskName": "生产环境 CVE 扫描",
    "targetIds": "1,2",
    "vulIds": "1001,1002",
    "status": "执行中",
    "totalItems": 4,
    "completedItems": 0,
    "failedItems": 0,
    "priority": 1,
    "startTime": null,
    "endTime": null,
    "resultSummary": null
  },
  "success": true
}
```

**状态说明：**

| status | 含义 |
|--------|------|
| 待执行 | 已入库，等待异步拆分 |
| 执行中 | 拆分完成，检测项已投递 RocketMQ |
| 已完成 | 所有检测项执行完毕（Redis 计数器达标） |
| 已取消 | 用户在待执行状态取消 |
| 异常终止 | 拆分阶段模板/资产拉取失败 |

---

## `GET /task` — 任务列表

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10，最大 100 |
| taskName | String | 否 | 模糊搜索任务名 |
| status | String | 否 | 状态过滤 |

```json
// GET /task?status=执行中&page=1
{
  "total": 3,
  "data": [{
    "taskId": 1, "taskName": "生产环境 CVE 扫描",
    "status": "执行中", "totalItems": 4,
    "completedItems": 0, "failedItems": 0,
    "priority": 1, "startTime": "2026-04-30T19:15:00", "endTime": null
  }]
}
```

---

## `GET /task/{taskId}` — 任务详情

```json
// GET /task/1
{
  "data": {
    "taskId": 1, "taskName": "生产环境 CVE 扫描",
    "targetIds": "1,2", "vulIds": "1001,1002",
    "status": "执行中",
    "totalItems": 4, "completedItems": 2, "failedItems": 0,
    "priority": 1,
    "startTime": "2026-04-30T19:15:00",
    "endTime": null,
    "resultSummary": null
  }
}
```

| 字段 | 说明 |
|------|------|
| totalItems | 检测项总数（M 资产 × N 模板） |
| completedItems | 已完成数（TaskProgressScheduler 轮询 Redis 回填） |
| startTime | 异步拆分开始时间 |
| endTime | 最后一个检测项完成时间 |
| resultSummary | 结果摘要 JSON：`{"matched":N,"notMatched":N,"error":N}` |

---

## `DELETE /task/{taskId}` — 取消任务

只能取消 **待执行** 状态的任务。

```json
// Response (成功): { "success": true }
// Response (失败): { "success": false, "error": {"code":2001,"message":"仅待执行状态的任务可取消"} }
```

---

## 内部流程（无 REST 接口）

```
POST /task 创建
  ↓
异步线程池 splitAndDispatch:
  1. TemplateCache(Caffeine+Redis) 拉取模板检测配置
  2. Feign asset-service 拉取资产信息
  3. M资产 × N模板 拆分 → task_item 持久化
  4. 构建 TaskItemMessage(含全量数据) → RocketMQ task_item_topic

TaskProgressScheduler 每 2s:
  GET Redis task:{taskId}:completed
  → completed >= total → task.status = DONE
```

---

## 错误码

| code | 含义 |
|------|------|
| 1002 | 任务不存在 |
| 2001 | 参数校验失败 / 状态不允许取消 |
| 5001 | 服务器内部错误 |
