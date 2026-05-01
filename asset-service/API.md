# Asset-Service API

> 资产管理。Base path 通过 Gateway: `/api/asset`

## 资产管理

### `GET /asset` — 分页列表

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数 |
| name | String | 否 | 模糊搜索名称 |
| riskLevel | String | 否 | 风险等级过滤 |
| status | String | 否 | 状态过滤 |
| categoryId | Long | 否 | 按分类过滤 |

```json
// GET /asset?size=2
{ "total": 2, "data": [{
    "assetId": 1, "name": "测试资产",
    "requestProtocol": "http", "requestHost": "localhost",
    "requestPort": 8001, "requestPath": "/",
    "requestMethod": "GET", "riskLevel": "UNKNOWN",
    "status": "DISABLED", "lastScanTime": null,
    "description": null
}]}
```

### `GET /asset/{assetId}` — 详情

```json
// GET /asset/1
{ "data": {
    "assetId": 1, "name": "测试资产",
    "requestProtocol": "http", "requestHost": "localhost",
    "requestPort": 8001, "requestPath": "/",
    "requestMethod": "GET", "requestHeader": null,
    "riskLevel": "UNKNOWN", "status": "DISABLED",
    "description": null, "lastScanTime": null
}}
```

### `POST /asset` — 创建

```json
// Request
{
  "name": "新增资产",
  "requestProtocol": "https",
  "requestHost": "api.example.com",
  "requestPort": 443,
  "requestPath": "/",
  "requestMethod": "GET",
  "categoryId": 1
}
```

### `PUT /asset/{assetId}` — 更新

Body 同创建，字段均可选。

### `DELETE /asset/{assetId}` — 删除

软删除。

---

## 分类管理

### `GET /asset-category` — 分类树

返回树形结构（含 children 递归）。

```json
// GET /asset-category
{ "data": [{
    "categoryId": 1, "name": "Web应用",
    "parentId": null,
    "children": []
}]}
```

### `POST /asset-category` — 创建

```json
// Request
{ "name": "新分类", "parentId": null, "description": "..." }
```

### `PUT /asset-category/{categoryId}` — 更新

含防循环校验。

### `DELETE /asset-category/{categoryId}` — 删除

有子节点或关联资产时拒绝。

### `POST /asset-category/{categoryId}/assets` — 批量关联资产

```json
// Request: { "assetIds": [1,2,3] }
```

### `DELETE /asset-category/{categoryId}/assets` — 批量取消关联

```json
// Request: { "assetIds": [1,2] }
```

---

## 错误码

| code | 含义 |
|------|------|
| 1002 | 资源不存在 |
| 2001 | 参数校验失败 |
| 2002 | 操作被拒绝 |
| 5001 | 服务器内部错误 |
