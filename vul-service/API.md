# Vul-Service API

> 漏洞知识库。Base path 通过 Gateway: `/api/vul`

## 模板管理

### `GET /vul` — 分页列表

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20，最大 100 |
| name | String | 否 | 模糊搜索名称 |
| severity | String | 否 | 精确过滤：critical/high/medium/low/info |
| tag | String | 否 | 标签名过滤 |
| categoryId | Long | 否 | 分类 ID 过滤 |
| enabled | Boolean | 否 | 启用状态过滤 |

```json
// GET /vul?size=2&severity=critical
{ "total": 10686, "records": [{
    "id": 1, "templateId": "cve-2024-0012",
    "name": "PAN-OS - Auth Bypass",
    "severity": "critical", "cveId": "CVE-2024-0012",
    "cvssScore": 9.80, "enabled": true, "version": 1,
    "tags": ["cve","paloalto"], "categories": ["cves"],
    "createTime": "2026-04-30T13:20:00"
}]}
```

### `GET /vul/{id}` — 详情

返回完整模板信息，含 `httpSteps`/`matchers`/`extractors`/`references`/`tags`/`categories`。

```json
// GET /vul/1001
{
  "id": 1001, "templateId": "cnvd-2017-03561",
  "name": "...", "description": "...", "author": "...",
  "severity": "high", "cveId": null, "cweId": null,
  "cvssScore": null, "epssScore": null, "flow": null,
  "variables": {"num1":"{{rand_int(800000,999999)}}"},
  "enabled": true, "version": 1,
  "tags": ["cnvd","lfi"], "categories": ["cnvd"],
  "references": [{"url":"https://...","title":null}],
  "httpSteps": [{
    "stepOrder": 1, "method": "GET",
    "path": ["{{BaseURL}}/login.do?message={{num1}}"],
    "headers": null, "body": null, "raw": null,
    "attack": null, "matchersCondition": "or",
    "matchers": [{
      "type":"word","part":"body","condition":"or",
      "negative":false,"caseInsensitive":false,"name":null,
      "config":{"words":["{{result}}"]}
    }],
    "extractors": []
  }]
}
```

### `POST /vul` — 创建模板

```json
// Request
{
  "templateId": "custom-001",
  "name": "自定义检测", "severity": "high",
  "tags": ["custom"], "categoryIds": [1],
  "httpSteps": [{
    "method": "GET", "path": ["{{BaseURL}}/admin"],
    "matchersCondition": "or",
    "matchers": [{"type":"word","part":"body","config":{"words":["admin"]}}]
  }]
}
// Response: { "data": 10001, "success": true }   ← 返回新模板 ID
```

### `PUT /vul/{id}` — 更新模板（部分更新）

Body 同创建，字段均可选。子表（tags/httpSteps/matchers/extractors）传入时先删后插。

### `DELETE /vul/{id}` — 软删除

有分类关联时拒绝（返回 403）。删除时级联删除所有子表数据。

### `PATCH /vul/{id}/enabled` — 启用/禁用

```json
// Request: { "enabled": false }
```

### `POST /vul/batch-delete` — 批量删除

```json
// Request: { "ids": [1,2,3] }
// Response: { "data": 2, "success": true }   ← 实际删除数
```

---

## 标签管理

### `GET /vul-tag` — 标签列表

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 模糊搜索 |

```json
// GET /vul-tag?keyword=cve
[{ "id": 1, "name": "cve", "templateCount": 5231 }]
```

标签只读，由模板创建/导入时自动维护。

---

## 分类管理

### `GET /vul-category` — 分类树

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| parentId | Long | 否 | 只查某个父节点下的子节点 |

```json
// GET /vul-category
[{ "categoryId": 1, "name": "cves", "parentId": null,
   "sortOrder": 0, "templateCount": 3964, "children": [] }]
```

### `POST /vul-category` — 创建

```json
// Request
{ "name": "新分类", "parentId": null, "sortOrder": 0, "description": "..." }
```

### `PUT /vul-category/{id}` — 更新

含防循环检查：不能把 parentId 设为自己的子孙节点。

### `DELETE /vul-category/{id}` — 删除

有子节点或关联模板时拒绝。

### `POST /vul-category/{id}/templates` — 关联模板

```json
// Request: { "templateIds": [1,2,3] }
```

### `DELETE /vul-category/{id}/templates` — 取消关联

```json
// Request: { "templateIds": [1,2] }
```

---

## 内部 Feign（task-service 调用）

### `GET /vul/internal/{id}`

返回精简的检测配置文件（`VulTemplateDetectDTO`），只含执行所需字段，不含 name/description/author 等元数据。

### `POST /vul/internal/batch`

```json
// Request: { "ids": [1,2,3] }
// Response: [VulTemplateDetectDTO, ...]
```

---

## 错误码

| code | 含义 |
|------|------|
| 1002 | 资源不存在 |
| 2001 | 参数校验失败 |
| 2002 | 操作被拒绝（如删除有关联的分类） |
| 5001 | 服务器内部错误 |
