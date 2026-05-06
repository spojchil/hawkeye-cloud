# Hawkeye Cloud 安全评估报告

> 生成时间：2026-05-04
> 基于漏洞模板库分析

---

## 一、项目技术栈

| 组件 | 版本 | 备注 |
|------|------|------|
| Spring Boot | 4.0.5 | 较新版本 |
| Spring Cloud | 2025.1.0 | 较新版本 |
| Spring Cloud Alibaba | 2025.1.0.0 | 包含Nacos |
| Druid | 1.2.38 | 数据库连接池 |
| Java | 21 | LTS版本 |
| MySQL | 9.1.0 | 数据库 |
| Redis | - | 缓存 |
| RocketMQ | 5.3.3 | 消息队列 |

---

## 二、发现的安全风险

### 🔴 高风险

#### 1. 默认凭证未修改

**问题**：项目配置文件中使用了默认凭证

| 组件 | 默认用户名 | 默认密码 | 配置位置 |
|------|-----------|---------|---------|
| MySQL | root | root | 所有服务的 application.yml |
| Redis | - | 123456 | gateway/auth/task/detection 服务 |
| Nacos | nacos | nacos | Nacos服务器（如果未修改） |

**相关漏洞模板**：
- `nacos-default-login` (High) - Alibaba Nacos 默认登录
- `redis-commander-default-login` (High) - Redis Commander 默认登录

**修复建议**：
```yaml
# 修改为强密码，使用环境变量
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      password: ${REDIS_PASSWORD}
```

#### 2. Nacos 认证绕过（需确认版本）

**问题**：Nacos 1.x 存在认证绕过漏洞

**相关漏洞模板**：
- `CVE-2021-29441` (Critical) - Nacos <1.4.1 认证绕过
- `nacos-auth-bypass` (Critical) - Nacos 1.x 认证绕过

**影响版本**：Nacos < 1.4.1

**修复建议**：
- 升级 Nacos 到 2.x 版本
- 启用认证：`nacos.core.auth.enabled=true`
- 修改默认凭证

---

### 🟡 中风险

#### 3. Spring Cloud Gateway Actuator 端点暴露

**问题**：如果启用了Actuator端点且未保护，可能存在安全风险

**相关漏洞模板**：
- `CVE-2025-41243` (Critical) - Spring Cloud Gateway Server Webflux 权限绕过

**影响版本**：Spring Cloud Gateway（需确认具体版本）

**修复建议**：
```yaml
# 禁用不需要的Actuator端点
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

#### 4. Druid Monitor 暴露（如果启用）

**问题**：Druid Monitor 默认凭证和未授权访问

**相关漏洞模板**：
- `druid-default-login` (High) - Druid Monitor 默认登录
- `druid-monitor` (High) - Druid Monitor 未授权访问

**影响版本**：所有版本

**修复建议**：
```yaml
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: false  # 生产环境禁用
        # 或修改默认凭证
        login-username: admin
        login-password: ${DRUID_PASSWORD}
        allow: 127.0.0.1  # 仅允许本地访问
```

---

### 🟢 低风险

#### 5. Spring4Shell (CVE-2022-22965)

**问题**：Spring MVC RCE漏洞

**影响条件**：
- Java 9+
- Tomcat WAR部署
- Spring MVC/WebFlux

**当前状态**：
- 项目使用 Spring Boot 4.0.5，应该已修复
- 项目使用 Java 21

**风险评估**：低

#### 6. Spring Cloud Config Server 路径遍历

**相关漏洞模板**：
- `CVE-2026-22739` (High) - Spring Cloud Config Server 路径遍历

**当前状态**：
- 项目未使用 Spring Cloud Config Server

**风险评估**：无风险

---

## 三、安全加固建议

### 立即执行（P0）

1. **修改所有默认凭证**
   - MySQL：修改root密码
   - Redis：设置强密码
   - Nacos：修改默认密码

2. **启用Nacos认证**
   ```properties
   nacos.core.auth.enabled=true
   nacos.core.auth.server.identity.key=your-key
   nacos.core.auth.plugin.nacos.token.secret.key=your-secret-key
   ```

### 尽快执行（P1）

3. **保护Actuator端点**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info
   ```

4. **禁用或保护Druid Monitor**
   - 生产环境禁用 Druid Monitor
   - 如果必须启用，修改默认凭证并限制访问IP

### 持续改进（P2）

5. **定期更新依赖**
   - 关注 Spring Cloud 安全公告
   - 定期更新 Druid 版本

6. **网络隔离**
   - 数据库、Redis、Nacos 不暴露公网
   - 使用防火墙限制访问

---

## 四、相关漏洞模板清单

| 模板ID | YAML ID | 名称 | 严重性 | 项目风险 |
|--------|---------|------|--------|---------|
| 4311 | nacos-default-login | Alibaba Nacos 默认登录 | High | 🔴 高 |
| 862 | nacos-auth-bypass | Nacos 1.x 认证绕过 | Critical | 🟡 中 |
| 227 | CVE-2021-29441 | Nacos <1.4.1 认证绕过 | Critical | 🟡 中 |
| 3908 | CVE-2025-41243 | Spring Cloud Gateway 权限绕过 | Critical | 🟡 中 |
| 4245 | druid-default-login | Druid Monitor 默认登录 | High | 🟡 中 |
| 6710 | druid-monitor | Druid Monitor 未授权访问 | High | 🟡 中 |
| 2602 | CVE-2022-22965 | Spring RCE | Critical | 🟢 低 |
| 2598 | CVE-2022-22947 | Spring Cloud Gateway 代码注入 | Critical | 🟢 低 |

---

## 五、检测建议

建议使用以下模板进行安全检测：

```bash
# 检测Nacos默认登录
nuclei -t nacos-default-login -u http://localhost:8848

# 检测Druid Monitor
nuclei -t druid-default-login -u http://localhost:8080
nuclei -t druid-monitor -u http://localhost:8080

# 检测Spring Cloud Gateway
nuclei -t CVE-2025-41243 -u http://localhost:8001
```
