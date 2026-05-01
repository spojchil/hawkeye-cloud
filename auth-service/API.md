# Auth-Service API

> JWT 认证服务。Base path 通过 Gateway: `/api/auth`

---

## `POST /auth/login` — 登录

**返回** `ApiResponse<LoginVO>`

```json
// Request
{ "username": "admin", "password": "123456" }

// Response
{
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  },
  "success": true
}
```

后续请求在 Header 中携带：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## 认证机制

| 项目 | 说明 |
|------|------|
| 算法 | HMAC-SHA256 (JJWT) |
| 过期 | 2 小时 |
| 校验位置 | Gateway AuthFilter |
| 黑名单 | Redis reactive，主动失效（logout） |
| 刷新 | 不支持，过期后重新登录 |

---

## 错误码

| code | 含义 |
|------|------|
| 1001 | 用户名或密码错误 |
| 1003 | Token 无效或已过期 |
| 1004 | Token 已失效（黑名单中） |
