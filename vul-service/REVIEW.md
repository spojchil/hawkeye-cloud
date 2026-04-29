# vul-service 审查报告

> 对照参考：`asset-service`（已审查修复）、`auth-service`

---

## 一、风格 / 架构对照

被审查分支：`feature/vul-service`，基准分支：`develop`

### 1.1 启动类

| 检查项 | asset | auth | **vul（有问题）** |
|--------|-------|------|-------------------|
| `@ComponentScan` | **无** — 通过 `AdditionalScanConfig.java` 配置类实现 | 有 — 写在启动类（旧写法，尚未改） | 有 — 写在启动类，且多扫了 `com.common.utils.mapper` 这个不存在的包 |
| `@MapperScan` | **无** — MyBatis-Plus `@Mapper` 注解自动注册 | 无 | 有 — 多扫了 `com.common.utils.mapper` |

**问题**：asset 已经改成用 `AdditionalScanConfig` 配置类单独管理扫描，vul 应该跟 asset 对齐：
```
asset 做法 → 删除启动类上的 @ComponentScan / @MapperScan，
             在 vul-common/config/ 下新建 AdditionalScanConfig
```

另外 `com.common.utils.mapper` 这个包在 common-utils 里**不存在**，写了白写。

### 1.2 模块 POM 对比

| 模块 | asset | **vul（问题）** |
|------|-------|----------------|
| `*-api/pom.xml` | 只声明依赖，**无** `<build>` 段 | 多了一段 `spring-boot-maven-plugin <skip>true</skip>` |
| `*-common/pom.xml` | 只声明依赖 + `jakarta.validation-api`，**无** `<build>` 段 | 少了 `jakarta.validation-api`；多了多余 `<build>` 段 |
| `*-business/pom.xml` | 只声明依赖，**无** `<build>` 段 | 多了多余 `<build>` 段 |

**问题**：父 POM 已经全局配置 `spring-boot-maven-plugin <skip>true</skip>`，子模块不需要重复声明。vul 的 api/common/business 三个 POM RPS 都多写了 `<build><plugins>` 段，重复且冗余。

### 1.3 `application.yml` 对比

| 配置项 | asset | **vul（差异）** |
|--------|-------|----------------|
| `datasource.driver-class-name` | ✅ `com.mysql.cj.jdbc.Driver` | ❌ **缺失** |
| MyBatis-Plus 注释 | ✅ 有详细迁移说明注释 | ❌ 无注释 |
| MyBatis `mapper-locations` | `classpath:mapper/**/*.xml` | `classpath:mapper/**/*.xml` ✅ |
| 多余空行 | 有少量 | 很少 ✅ |

**问题**：`driver-class-name` 虽非必须（Spring Boot 可自动推断），但 asset/auth 两个已有服务都保留了，vul 应该补上保持一致性。

### 1.4 SQL 文件位置

| 服务 | SQL 文件位置 |
|------|-------------|
| asset | `asset-service/asset-service.sql`（**服务根目录**） |
| auth | `auth-service/account.sql`（**服务根目录**） |
| **vul** | `vul-bootstrap/src/main/resources/sql/vul_ddl.sql`（bootstrap 模块内部） |

**问题**：SQL 应放在服务根目录 `vul-service/vul-ddl.sql`，与 asset、auth 保持一致。放在 `resources/sql/` 下会被 maven-resources-plugin 拷贝到 jar 包中，线上包携带 DDL 不妥。

### 1.5 缺少 `AdditionalScanConfig`

asset 通过 `asset-common/config/AdditionalScanConfig.java` 独立配置包扫描。vul 没有这个文件，导致扫描逻辑被迫写在启动类上。应补一个。

---

## 二、代码正确性

### 🔴 严重

**2.1 `VulVO.Request` 零校验**
`vul-common/.../vo/vul/VulVO.java:10-27`

```java
public static class Request {
    private String templateId;  // 无 @NotBlank
    private String name;        // 无 @NotBlank
    private String severity;    // 无任何约束
    ...
}
```
asset 的 `AssetVO.Request` 有 `@NotBlank`、`@NotNull` 保护所有必填字段。vul 一条校验都没有，空名称、空严重程度直接入库。

**修复**：参照 `AssetVO.Request` 给必填字段加 `@NotBlank` / `@NotNull`，并在 Controller 的 create/update 方法上加 `@Valid`（已有）。

---

**2.2 `VulTemplateServiceImpl.delete()` 静默删除关联**
`vul-business/.../impl/VulTemplateServiceImpl.java:143-150`

```java
// 当前代码：有关联不报错，悄悄删
if (mappingCount > 0) {
    vulCategoryMappingMapper.delete(...);  // 级联删除
}
```
asset 做法：
```java
// asset 做法：有关联 → OPERATION_DENIED
if (mappingCount > 0) {
    throw new ApiException(OPERATION_DENIED, "请先移除所有关联再删除");
}
```

**修复**：要么改成拒绝删除（推荐，与 asset 一致），要么在注释里说明这是有意为之。

---

**2.3 `flushBatch()` N+1 查重**
`vul-business/.../impl/VulTemplateImportServiceImpl.java:156-166`

每条模板 insert 前单独 `selectOne`。10,688 个模板就是 10,000+ 次查询。
映射表同理（第 174 行）。

**修复**：改为批量 IN 查询 — 把 batch 中所有 `templateId` 一次 IN 查出来，在内存中对比去重。

---

**2.4 Severity 类型矛盾**
`VulSeverityEnum` 实现 `IEnum<Integer>`，暗示 DB 存 TINYINT。但实体字段是 `String severity`，DDL 是 `VARCHAR(20)`，实际存储 `"low"` / `"high"` 字符串。`IEnum<Integer>` 实现完全无效。

**修复**（二选一）：
- 改实体字段为 `VulSeverityEnum severity`，DDL 改 `TINYINT`，走 MybatisEnumTypeHandler 自动映射
- 或者去掉 `implements IEnum<Integer>`，改用普通枚举 + 手动转换

---

### 🟡 中等

**2.5 写方法缺 `@Transactional`**
`VulTemplateServiceImpl.create()` / `update()` — 无事务
`VulCategoryServiceImpl.create()` / `update()` — 无事务
（`delete()` 和 `addTemplates()` 已有）

资产服务审查时已修复过同样问题，vul 应参照补上。

---

**2.6 `VulCategoryServiceImpl.update()` 用 entity 更新而非 `LambdaUpdateWrapper`**
`vul-business/.../impl/VulCategoryServiceImpl.java:71-80`

资产服务统一使用 `LambdaUpdateWrapper.set(condition, col, val)` 做部分更新。vul 这里直接用 `baseMapper.updateById(category)` 做全量更新，风格不统一。而且 `isAncestor()` 树遍历（逐层 selectById）也在事务外，有竞态风险。

---

**2.7 Tags LIKE 不走 FULLTEXT 索引**
DDL 建了 `FULLTEXT KEY ft_tags`（`vul_ddl.sql:36`），但代码用的是 `LIKE '%keyword%'`。MySQL 中 FULLTEXT 索引对 LIKE 无效 —— 只有 `MATCH ... AGAINST` 才能用。目前 tags LIKE 是全表扫描。

**修复**：要么改用 `MATCH ... AGAINST`，要么删掉无用的 FULLTEXT 索引。

---

**2.8 `update()` 缺全 null 校验**
资产服务已修的这个 bug 在 vul 里再次出现 — 所有字段 null → SQL 变成 `UPDATE SET WHERE id=?`。

---

**2.9 导入服务无事务**
`VulTemplateImportServiceImpl` 全程无 `@Transactional`，中途失败数据不回滚。

---

### 🟢 风格

**2.10 Service 类缺 Javadoc** — `VulTemplateServiceImpl` / `VulCategoryServiceImpl` 没有类级文档（资产服务每个类都有）

**2.11 变量名过长** — `vulCategoryMappingMapper`，资产已统一为 `mappingMapper`

**2.12 Controller delete 写法** — `return ApiResponse.success(null)` 应写作 `return ApiResponse.success()`（参照 asset/auth）

**2.13 缺 `@LogExecutionTime`** — 资产服务刚补上的方法级日志，vul 全无

**2.14 `VulVO.Request` 和 `VulVO.Response` 字段列表完全相同** — 没有像 `AssetVO.Response` 那样只返回需要的字段。Response 直接暴露了所有敏感字段。

**2.15 VulVO 字段在 Request / Response / Entity 中的命名不统一**
- Entity: `Long id`，`String templateId`
- VO Request: `String templateId`（无 id）
- VO Response: `Long id`，`String templateId`
- PageVO Request: 无这两个字段（用 `categoryId`）
- PageVO Response: `Long id`，`String templateId`

和其他服务对比：asset 的 AssetVO.Response 用 `assetId`，Asset 实体用 `assetId` — 完全一致。vul 的 entity 中业务主键叫 `templateId`，自增主键叫 `id`，VO 里混着暴露这两个 ID，容易混淆。

---

## 三、对比清单速查表

| # | 类别 | 问题 | 严重度 |
|---|------|------|--------|
| 1.1 | 架构 | 启动类 @ComponentScan + @MapperScan 应改为 AdditionalScanConfig | 中 |
| 1.2 | 架构 | api/common/business 的 POM 多了冗余 `<build>` 段 | 低 |
| 1.3 | 架构 | yml 缺 `driver-class-name`、无 MP 迁移注释 | 低 |
| 1.4 | 架构 | DDL 放错了位置（应在服务根目录而非 resources 内） | 中 |
| 1.5 | 架构 | 缺 AdditionalScanConfig | 中 |
| 2.1 | 正确性 | VulVO.Request 零校验 | 🔴 严重 |
| 2.2 | 正确性 | delete 模板时静默删除分类关联 | 🔴 严重 |
| 2.3 | 性能 | flushBatch N+1（10000+ 次单条 select） | 🔴 严重 |
| 2.4 | 正确性 | severity 类型矛盾（IEnum\<Integer\> 但实际上存 VARCHAR） | 🔴 严重 |
| 2.5 | 规范 | 写方法缺 @Transactional | 中 |
| 2.6 | 规范 | Category update 用 entity 而非 LambdaUpdateWrapper | 中 |
| 2.7 | 性能 | Tags LIKE 不走 FULLTEXT 索引 | 中 |
| 2.8 | 正确性 | update 缺全 null 校验 | 中 |
| 2.9 | 规范 | 导入服务无事务 | 中 |
| 2.10 | 风格 | Service 缺 Javadoc | 低 |
| 2.11 | 风格 | 变量名过长 | 低 |
| 2.12 | 风格 | 控制器 delete 写法 | 低 |
| 2.13 | 风格 | 缺 @LogExecutionTime | 低 |
| 2.14 | 风格 | Response VO 暴露所有字段 | 低 |
| 2.15 | 风格 | Entity 中的 id 命名不一致（templateId / id） | 低 |
