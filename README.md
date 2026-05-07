# Hawkeye Cloud

分布式漏洞检测平台 —— 基于 Java 21 + Spring Cloud 微服务 + RocketMQ 异步调度，支持 SaaS 多租户。

[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 快速开始

```bash
# 打包
mvn clean package -DskipTests

# 一键启动全部服务（12 个容器）
docker compose up -d --build
```

| 入口 | 地址 |
|------|------|
| 管理界面 | http://localhost:3000 |
| 网关入口 | http://localhost:8001 |
| Nacos 控制台 | http://localhost:8848/nacos （nacos / nacos） |

前置依赖：JDK 21、Maven 3.9+、Docker。

---

## 架构

```
  Browser → Gateway (:8001) ──→ Auth (:8002)   登录认证
            │                   Asset (:8003)  资产管理
            │                   Vul (:8004)    漏洞模板
            │                   Task (:8005)   任务调度
            └─ RocketMQ ────→ Detection (:8006+) Worker 集群
```

| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8001 | API 网关：路由转发 + JWT 鉴权 |
| auth | 8002 | 认证：登录 + JWT 签发 |
| asset | 8003 | 资产管理：CRUD + 分类树 |
| vul | 8004 | 漏洞模板：12 表归一化 + YAML 导入 |
| task | 8005 | 任务调度：提交 → 拆分 → MQ 分发 → 轮询进度 |
| detection | 8006+ | 检测 Worker：匹配引擎 + 管线模式，可水平扩展 |
| web-admin | 3000 | 管理界面（SPA 单页） |

每个微服务内部 `common → business → api → bootstrap` 四层分包，依赖单向。

### 核心检测链路

```
用户提交任务 → task 拆分(资产×模板) → RocketMQ 异步投递 → detection 并发探测 → 批量回写
```

---

## 技术栈

Java 21 · Spring Boot 4.0 · Spring Cloud Alibaba · MyBatis-Plus · MySQL · Redis · RocketMQ · Aviator · Caffeine · MapStruct · Docker · GitHub Actions

---

## 项目结构

```
hawkeye-cloud/
├── common-service/        # 公共基础设施
├── gateway-service/       # API 网关
├── auth-service/          # 认证服务
├── asset-service/         # 资产服务
├── vul-service/           # 漏洞管理服务
├── task-service/          # 任务调度服务
├── detection-service/     # 检测执行服务
├── web-admin/             # 管理界面
├── docs/                  # 项目文档 & API 文档 & SQL
└── docker-compose.yml     # 容器编排
```

---

## 文档

| 文档 | 说明 |
|------|------|
| [项目说明](docs/项目说明.md) | 定位、技术栈、进度 |
| [架构设计](docs/架构设计.md) | 整体架构、多租户、检测链路 |
| [架构演化](docs/架构演化.md) | v1→v3 设计决策与迭代历史 |
| [API 文档](docs/api/) | 各服务 REST API |
| [SQL 脚本](docs/sql/) | 各服务 DDL |
| [开发规范](docs/开发规范补充.md) | 阿里规范 + 项目约定 |

## 漏洞模板库

兼容 [Nuclei Templates](https://github.com/projectdiscovery/nuclei-templates) 格式，**10,000+** 现成检测模板。

## 许可证

[MIT License](LICENSE) · Nuclei Templates [MIT License](https://github.com/projectdiscovery/nuclei-templates/blob/main/LICENSE.md)
