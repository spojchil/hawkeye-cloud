# Hawkeye Cloud

分布式漏洞检测平台 —— 基于 Java 21 + Spring Cloud 微服务架构，支持 SaaS 多租户与私有化部署。

## 快速开始

```bash
# 打包
mvn clean package -DskipTests

# Docker Compose 一键启动（MySQL + Redis + Nacos + 所有微服务）
docker compose up -d

# 网关入口：http://localhost:8000
# Nacos 控制台：http://localhost:8848/nacos
```

前置依赖：JDK 21、Maven 3.9+、Docker。

## 服务列表

| 服务 | 端口 | 状态 | 说明 |
|------|------|------|------|
| gateway-service | 8001 | 基础 | API 网关：路由转发 + JWT 鉴权 + CORS |
| auth-service | 8002 | 基础 | 用户认证：登录 + JWT 签发 |
| asset-service | 8003 | 完成 | 资产管理：CRUD + 分类树 |
| vul-service | 8004 | 完成 | 漏洞模板管理：CRUD + YAML 导入 + 分类 + 标签 |
| task-service | 8005 | 完成 | 任务调度引擎：提交 → 拆分 → RocketMQ 分发 → 轮询进度 |
| detection-service | 8006+ | 完成 | 检测 Worker：HTTP 探测 + 匹配引擎，可水平扩展 |
| tenant-service | — | 规划中 | 多租户管理 |
| audit-service | — | 规划中 | 日志审计服务 |

## 项目结构

```
hawkeye-cloud/
├── common-service/        # 公共基础设施（统一响应、多租户、异常处理）
├── gateway-service/       # API 网关
├── auth-service/          # 认证服务
├── asset-service/         # 资产服务
├── vul-service/           # 漏洞管理服务
├── task-service/          # 任务调度服务
├── detection-service/     # 检测执行服务
├── web-admin/             # 前端管理界面
├── docs/                  # 项目文档
└── docker-compose.yml     # 容器编排
```

每个微服务内部按 `common → business → api → bootstrap` 四层分包，依赖单向。

## 文档

### 项目文档

| 文档 | 说明 |
|------|------|
| [项目说明](docs/项目说明.md) | 项目定位、技术栈、开发进度 |
| [架构设计](docs/架构设计.md) | 整体架构、网关、多租户、检测链路 |


| [开发规范补充](docs/开发规范补充.md) | 阿里开发规范 + 项目特定约定 |

### 模块文档

| 模块 | 文档 |
|------|------|
| 公共服务 | [模块01-公共服务](docs/模块01-公共服务.md) |
| 网关服务 | [模块02-网关服务](docs/模块02-网关服务.md) |
| 认证服务 | [模块03-认证服务](docs/模块03-认证服务.md) |
| 资产服务 | [模块04-资产服务](docs/模块04-资产服务.md) |
| 漏洞管理服务 | [模块05-漏洞管理服务](docs/模块05-漏洞管理服务.md) |
| 任务调度服务 | [模块06-任务调度服务](docs/模块06-任务调度服务.md) |
| 检测执行服务 | [模块07-检测执行服务](docs/模块07-检测执行服务.md) |

### API 文档

| 服务 | 文档 |
|------|------|
| 认证服务 | [API](docs/api/认证服务-API.md) |
| 资产服务 | [API](docs/api/资产服务-API.md) |
| 漏洞管理服务 | [API](docs/api/漏洞管理服务-API.md) |
| 任务调度服务 | [API](docs/api/任务调度服务-API.md) |
| 检测执行服务 | [API](docs/api/检测执行服务-API.md) |

### SQL 脚本

| 服务 | 脚本 |
|------|------|
| 认证服务 | [SQL](docs/sql/认证服务.sql) |
| 资产服务 | [SQL](docs/sql/资产服务.sql) |
| 漏洞管理服务 | [SQL](docs/sql/漏洞管理服务.sql) |
| 任务调度服务 | [SQL](docs/sql/任务调度服务.sql) |
| 任务服务迁移 | [SQL](docs/sql/任务服务-迁移v3到v4.sql) |

## 漏洞模板库

使用 [projectdiscovery/nuclei-templates](https://github.com/projectdiscovery/nuclei-templates) 的 HTTP 模板库，平台上线即可获得 **10,000+** 现成检测模板，覆盖 CVE、信息泄露、配置错误、未授权访问等主流漏洞类型。

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

nuclei-templates 模板库基于 [MIT License](https://github.com/projectdiscovery/nuclei-templates/blob/main/LICENSE.md) 授权使用。
