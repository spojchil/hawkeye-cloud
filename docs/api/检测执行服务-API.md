# Detection-Service

> **纯 Worker，无 REST API。** 通过 RocketMQ 消费 `task_item_topic`，启动端口 8006（健康检查+Nacos注册）。

| 配置项 | 值 |
|--------|-----|
| Topic | `task_item_topic` |
| ConsumerGroup | `task_item_consumer_group` |
| 消费模式 | CLUSTERING |
| NameServer | `localhost:9876` |
| 最大线程 | 64 |
| 重试 | 16 次递增延迟 → DLQ |

---

## 消息入口

```
Topic:        task_item_topic
ConsumerGroup: task_item_consumer_group
消费模式:      CLUSTERING
最大线程:      64
重试:          16 次递增延迟 → %DLQ% 死信
```

---

## TaskItemMessage 消息体

task-service 投递时携带完整执行数据，detection 零外部依赖。

```json
{
  "taskId": 1, "itemId": 5, "tenantId": 1,
  "createdAt": 1746000000000,

  "assetProtocol": "http",
  "assetHost": "localhost",
  "assetPort": 8001,
  "assetPath": "/",

  "templateId": "cnvd-2017-03561",
  "flow": null,
  "variables": {"num1": "{{rand_int(800000,999999)}}"},
  "httpSteps": [{
    "stepOrder": 1,
    "method": "GET",
    "path": ["{{BaseURL}}/login.do?message={{num1}}"],
    "headers": null, "body": null, "raw": null,
    "attack": null, "matchersCondition": "or",
    "matchers": [{
      "type": "word", "part": "body", "condition": "or",
      "negative": false, "caseInsensitive": false,
      "config": {"words": ["{{result}}"]}
    }],
    "extractors": []
  }]
}
```

---

## 内存组件

| 组件 | 职责 |
|------|------|
| `TaskItemConsumer` | RocketMQ 监听器，收到消息 → DetectionEngine |
| `DetectionEngine` | 编排：变量解析 → HTTP 探测 → 匹配 → 结果写入 |
| `VariableResolver` | `{{BaseURL}}`/`{{Hostname}}`/`{{rand_int}}` 等占位符递归替换 |
| `HttpExecutor` | Java HttpClient，支持 Simple(path+method) 和 Raw(原始HTTP文本) 两种模式 |
| `MatcherChain` | Word/Status/Dsl 三种匹配器责任链，支持 and/or/negative |
| `ExtractorChain` | Regex/Kval 提取器，多步骤间变量传递 |
| `ResultWriter` | 缓冲 500 条/5s 定时 → 批量 INSERT detection_result + Redis INCR |

---

## 执行流程

```
TaskItemConsumer.onMessage(msg)
  │
  ├─ DetectionEngine.execute(msg)
  │     ├─ VariableResolver(protocol, host, port, path, variables)
  │     ├─ flow==null → 单步 / "http(1)&&http(2)" → 串行多步
  │     │     ├─ resolve() 递归替换占位符
  │     │     ├─ raw≠null → executeRaw() / else → execute()
  │     │     ├─ ExtractorChain.extract() → 变量写回 resolver
  │     │     └─ MatcherChain.match() → matched/not_matched
  │     └─ → DetectionResult {matched/not_matched/error}
  │
  ├─ ResultWriter.write(result)
  │     ├─ buffer.add(result)
  │     ├─ Redis INCR task:{taskId}:{status}
  │     └─ buffer≥500 || 5s → batch INSERT detection_result
  │
  └─ 每 5s: Redis SET worker:load:{workerId}
```

---

## 错误处理

| 场景 | 行为 |
|------|------|
| 消息反序列化失败 | RocketMQ 重试 → DLQ |
| URI 非法字符(SQL注入模板等) | try-catch → URLEncoder 编码降级 |
| HTTP 超时/IOException | 写 error 结果，不抛异常 |
| Redis 不可用 | INCR 失败时静默跳过，批量写入仍正常 |
| 模板变量嵌套引用 | 最多 5 次递归解析，防死循环 |

---

## 数据写入

```sql
-- 每条检测项一条记录，task_item_id 唯一键保证幂等
INSERT INTO detection_result
  (task_id, task_item_id, template_id, asset_id,
   status, response_status_code, response_size,
   response_summary, matched_matcher, matched_at,
   error_message, duration_ms, tenant_id)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

---

## 无 REST 接口

detection-service 不提供任何 REST 端点。TemplateFetcher（Caffeine L1 + Redis L2）已从 detection 移除，缓存职责在 task-service。

如需手动触发单次探测，建议在 task-service 加一个 `/verify` 端点，而非在 detection 暴露接口。
