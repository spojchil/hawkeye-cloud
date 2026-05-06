# Vul-Service API

> 漏洞知识库。Base path 通过 Gateway: `/api/vul`
>
> 统一响应格式：`ApiResponse<T>` — `{ "data": T, "success": true, "status": "200 OK" }`

## 模板管理

### `GET /vul` — 分页列表

**返回** `ApiResponse<IPage<VulTemplatePageVO>>`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 默认 1 |
| size | int | 否 | 默认 20，最大 100 |
| name | String | 否 | 模糊搜索 |
| severity | String | 否 | critical/high/medium/low/info |
| tag | String | 否 | 标签名过滤 |
| categoryId | Long | 否 | 分类过滤 |
| enabled | Boolean | 否 | 启用状态 |

```json
{ "total": 10686, "records": [{
    "id": 1, "templateId": "cve-2024-0012",
    "name": "PAN-OS - Auth Bypass", "severity": "critical",
    "cveId": "CVE-2024-0012", "cvssScore": 9.80,
    "enabled": true, "version": 1,
    "tags": ["cve"], "categories": ["cves"],
    "createTime": "2026-04-30T13:20:00"
}]}
```

### `GET /vul/{id}` — 详情

**返回** `ApiResponse<VulTemplateDetailVO>`

含 `httpSteps`/`matchers`/`extractors`/`references`/`tags`/`categories`。

### `POST /vul` — 创建

**返回** `ApiResponse<Long>` — 新模板 ID

```json
// Request
{ "templateId":"custom-001", "name":"自定义", "severity":"high",
  "tags":["custom"], "categoryIds":[1],
  "httpSteps":[{ "method":"GET", "path":["{{BaseURL}}/admin"],
    "matchersCondition":"or",
    "matchers":[{"type":"word","part":"body","condition":"or",
      "config":{"words":["admin"]}}]
  }]
}
```

### `PUT /vul/{id}` — 更新（部分更新）

**返回** `ApiResponse<Void>`

Body 同创建，字段均可选。子表传入时先删后插。

### `DELETE /vul/{id}` — 删除

**返回** `ApiResponse<Void>`

有分类关联时返回 403。

### `PATCH /vul/{id}/enabled` — 启用/禁用

**返回** `ApiResponse<Void>`

```json
// Request: { "enabled": false }
```

### `POST /vul/batch-delete` — 批量删除

**返回** `ApiResponse<Integer>` — 实际删除数

```json
// Request: { "ids": [1,2,3] }
```

---

## 标签管理

### `GET /vul-tag` — 列表

**返回** `ApiResponse<List<VulTagVO>>`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 模糊搜索 |

```json
[{ "id": 1, "name": "cve", "templateCount": 5231 }]
```

---

## 分类管理

### `GET /vul-category` — 分类树

**返回** `ApiResponse<List<VulCategoryVO>>`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parentId | Long | 否 | 只查某个父节点下 |

```json
[{ "categoryId": 1, "name": "cves", "parentId": null,
   "sortOrder": 0, "templateCount": 3964, "children": [] }]
```

### `POST /vul-category` — 创建

**返回** `ApiResponse<Long>` — 新分类 ID

```json
// Request
{ "name":"新分类", "parentId":null, "sortOrder":0, "description":"..." }
```

### `PUT /vul-category/{id}` — 更新

**返回** `ApiResponse<Void>`

含防循环检查。

### `DELETE /vul-category/{id}` — 删除

**返回** `ApiResponse<Void>`

有子节点或关联模板时拒绝（403）。

### `POST /vul-category/{id}/templates` — 关联模板

**返回** `ApiResponse<Integer>`

```json
// Request: { "templateIds": [1,2,3] }
```

### `DELETE /vul-category/{id}/templates` — 取消关联

**返回** `ApiResponse<Integer>`

```json
// Request: { "templateIds": [1,2] }
```

---

## 内部 Feign

### `GET /vul/internal/{id}`

**返回** `ApiResponse<VulTemplateDetectDTO>`

精简检测配置，只含执行所需字段。task-service 调用。

### `POST /vul/internal/batch`

**返回** `ApiResponse<List<VulTemplateDetectDTO>>`

```json
// Request: { "ids": [1,2,3] }
```

---

## 错误码

| code | 含义 |
|------|------|
| 1002 | 资源不存在 |
| 2001 | 参数校验失败 |
| 2002 | 操作被拒绝 |
| 5001 | 服务器内部错误 |
