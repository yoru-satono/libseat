# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

LibSeat — 高校图书馆座位预约管理系统。技术栈：

| 组件 | 目录 | 技术 |
|------|------|------|
| 后端 | `server/` | Spring Boot 4.0.6 · Java 25 · PostgreSQL 17 |
| 前端 | `web/` | Vue 3.5 · Vite 8 · Tailwind CSS 4 · Pinia 3 · TypeScript 6 |
| E2E 测试 | `test/` | Python 3.14 · pytest 9 · httpx |

## 常用命令

### 基础设施

```bash
# 启动 PostgreSQL 17 + Mailpit（从项目根目录）
docker compose up -d

# 停止
docker compose down
```

数据库初始化脚本在 `server/sql/init/`，容器首次启动时自动执行。

### 后端

```bash
cd server

# 编译（跳过测试）
mvn compile -q

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=SeatRepositoryTest

# 启动应用（db 和 mailpit 容器需已运行）
mvn spring-boot:run
```

### 前端

```bash
cd web

# 安装依赖
npm install

# 开发模式（热重载，API 直连 localhost:8080）
npm run dev

# 类型检查 + 构建
npm run build

# Vitest 单元测试
npm run test

# Playwright E2E 测试
npx playwright test
```

### E2E API 测试

```bash
cd test

# 运行全部
uv run pytest

# 单个模块
uv run pytest tests/test_auth.py
```

后端需已在 `http://localhost:8080` 运行。

## 架构概览

### 项目结构

```
libseat/
├── server/
│   ├── src/main/java/com/libseat/
│   │   ├── common/       # Result<T>, ErrorCode, BusinessException
│   │   ├── config/        # SecurityConfig, CorsConfig
│   │   ├── controller/    # REST 控制器（8 个）
│   │   ├── dto/           # 请求/响应 DTO + PageResult<T>
│   │   ├── entity/        # JPA 实体 + 枚举类型
│   │   ├── exception/     # BusinessException, GlobalExceptionHandler
│   │   ├── repository/    # Spring Data JPA Repository
│   │   ├── scheduler/     # 定时任务
│   │   ├── security/      # JWT 过滤器, 认证/鉴权处理器
│   │   └── service/       # 业务服务层（8 个 Service + impl/）
│   ├── sql/init/          # 01_schema.sql + 02_seed.sql
│   ├── docs/              # API 规范、详细设计、概要设计
│   └── pom.xml
├── web/
│   ├── src/
│   │   ├── api/           # Axios 封装 + 各模块 API 函数 + admin/
│   │   ├── components/    # 可复用组件
│   │   ├── layouts/       # AuthLayout, UserLayout, AdminLayout
│   │   ├── pages/         # auth/, user/, admin/ 三组页面
│   │   ├── router/        # Vue Router 配置（路由守卫）
│   │   ├── stores/        # Pinia stores（auth, notifications）
│   │   ├── types/         # TypeScript 类型定义
│   │   └── utils/         # 工具函数
│   ├── e2e/               # Playwright E2E 测试
│   └── docs/              # 前端设计文档
├── test/
│   ├── tests/             # 按模块的 E2E 测试文件
│   ├── helpers/           # 常量和辅助函数
│   └── conftest.py        # pytest fixtures（anon, admin, user1, user2）
├── docs/                  # 产品需求/用户需求说明书 (PDF)
├── docker-compose.yml     # PostgreSQL + Mailpit + 生产容器
└── .env                   # 环境变量
```

### 跨组件约定

- **API 前缀**: 所有接口 `/api/v1`，URL 用复数名词 + 连字符（`/change-requests`）
- **状态码**: 所有接口 HTTP 200，业务结果通过 `code` 字段判断（`"00000"` 为成功）
- **分页**: page 从 1 开始，默认 pageSize=20，最大 100
- **时间格式**: ISO 8601 带时区，camelCase 字段名
- **错误码**: 5 位分层 — `A0xxx` 用户端 / `B0xxx` 业务规则 / `C0xxx` 服务端

---

## 后端 (server/)

### 分层结构

`controller → service → repository → entity`

### 统一响应格式

所有接口必须包装在 `Result<T>`（`common/Result.java`）中，禁止直接返回裸对象。

```json
{
  "code": "00000",
  "message": "ok",
  "data": { ... }
}
```

业务错误抛出 `BusinessException(ErrorCode.XXX)`，由 `GlobalExceptionHandler` 统一转换。Spring Security 的 401/403 在过滤器层抛出，需在 SecurityConfig 中通过 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 单独处理，同样返回 `Result` 格式。

### 数据库 Schema

`ddl-auto=none`，Hibernate 不操作 Schema。DDL 在 `sql/init/01_schema.sql`，种子数据在 `02_seed.sql`。compose.yml 将目录挂载为 Postgres 初始化脚本。

**PostgreSQL 枚举映射:**

```java
@Enumerated(EnumType.STRING)
@JdbcType(PostgreSQLEnumJdbcType.class)
@Column(columnDefinition = "your_enum_type")
```

DDL 中 ENUM 值必须大写（与 `enum.name()` 对齐）。

### 集成测试

所有 Repository 测试继承 `RepositoryTestBase`，通过 Testcontainers 启动真实 Postgres 17 容器，`pom.xml` 将 `sql/` 映射到测试 classpath `db/`。每个测试方法在事务中运行并自动回滚。

### JWT 认证

双 Token：`accessToken`（2h）+ `refreshToken`（7d）。请求头：`Authorization: Bearer <accessToken>`。access token 过期返回 `A0201`，客户端调 `/auth/refresh`；refresh token 过期返回 `A0202` 则重新登录。

### 关键领域规则

- **SystemRules**: `library_id = null` = 全局规则，非 null = 库级覆盖。取规则时优先库级 → 回退全局。
- **Reservation.parent**: 续约时新建预约通过 `parent` 指向被续约记录。
- **登录锁定**: `failedLoginCount` ≥ 5 → `lockedUntil`（锁定 30 分钟），在 `AuthService` 中。
- **爽约暂停**: `noShowCount` 超过 `noShowThreshold` → 按 `suspendDays` 设 `suspendedUntil`。
- **`@Modifying`**: 必须 `clearAutomatically = true`，否则同一事务内后续查询命中一级缓存。
- **Lombok + Boolean 包装类**: 用 `Boolean.TRUE.equals(x.getXxx())` 避免 NPE。
- **Jackson 包名**: Spring Boot 4.x 用 Jackson 3.x，包前缀 `tools.jackson`（非 `com.fasterxml`）。

### 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | A000001 | password |
| 学生 | S202401001 | password |

---

## 前端 (web/)

### 技术要点

- **构建**: Vite 8 + `@vitejs/plugin-vue`
- **CSS**: Tailwind CSS 4（`@tailwindcss/vite` 插件），无 `tailwind.config` 文件
- **图标**: Heroicons vue (`@heroicons/vue`)
- **日期**: dayjs + `@vuepic/vue-datepicker`
- **Toast**: `vue-toastification`
- **路径别名**: `@` → `src/`

### 路由结构

三套布局（`meta.layout`）:

| Layout | 路由前缀 | 页面数 | 认证要求 |
|--------|---------|--------|---------|
| `auth` | `/login`, `/register`, `/activate`, `/forgot-password`, `/reset-password`, `/email/confirm` | 6 | guest |
| `user` | `/`, `/seats`, `/reservations`, `/waitlists`, `/notifications`, `/profile` | 6 | 部分需登录 |
| `admin` | `/admin/*` (dashboard, users, reservations, change-requests, libraries, seats, system-rules, audit-logs) | 9 | ADMIN 角色 |

路由守卫在 `router/index.ts` 的 `beforeEach` 中：自动恢复用户信息、验证 admin 权限、未登录重定向 `/login`。

### API 层 (`src/api/`)

- `http.ts` — Axios 实例，baseURL 默认 `http://localhost:8080/api/v1`，VITE_API_BASE_URL 环境变量可覆盖
- **Token 刷新**: 拦截器自动处理 `A0201` → 调 `/auth/refresh` → 重试原请求，并发请求排队的刷新队列
- **自动登出**: `A0100`（未认证）、`A0202`（refresh 过期）→ 清除 token 跳转登录页
- 业务错误 `code != "00000"` 抛 `ApiError(code, message)`
- admin 模块 API 在 `api/admin/` 子目录

### Store (`src/stores/`)

- `auth.ts` — token 管理、用户信息、`isAdmin` getter、refresh 逻辑
- `notifications.ts` — 未读通知计数

### 测试

- **Vitest**: 环境 jsdom，全局 `describe`/`it`/`expect`，setup 文件 `src/test/setup.ts`，测试文件 `src/**/*.spec.ts`
- **Playwright**: E2E 测试在 `e2e/`，`playwright.config.ts` 配置

---

## E2E 测试 (test/)

### 运行

```bash
cd test
uv run pytest           # 全部（需后端运行）
uv run pytest -k auth   # 按关键字筛选
```

### Fixture 体系

`conftest.py` 提供 4 个 session 级 fixtures：

| Fixture | 身份 | 说明 |
|---------|------|------|
| `anon` | 匿名 | 无 token |
| `admin` | A000001 | 管理员 |
| `user1` | S202401001 | 王小明 ACTIVE |
| `user2` | S202401002 | 李小红 ACTIVE |

所有返回 `httpx.Client`，已设置 baseURL 和 Authorization header。

### 测试模块

| 文件 | 覆盖范围 |
|------|---------|
| `test_auth.py` | 登录/注册/激活/密码重置/刷新 |
| `test_seats.py` | 座位浏览/搜索/筛选 |
| `test_reservations.py` | 预约 CRUD/签到/签退/续约 |
| `test_notifications.py` | 通知列表/标记已读 |
| `test_users.py` | 个人信息/修改申请 |
| `test_admin_users.py` | 管理员用户 CRUD |
| `test_admin_reservations.py` | 管理员预约管理 |
| `test_admin_system.py` | 系统规则/图书馆/审计日志 |

### 常量

`helpers/constants.py` 包含所有种子数据的 UUID、账号、密码。

---

## 重要约定

1. **不要用 Hibernate 自动建表** — Schema 变更写 SQL 到 `sql/init/`
2. **所有 Controller 返回 `Result<T>`** — 不允许裸对象
3. **分页页码从 1 开始** — 前后端统一
4. **ENUM 值大写** — DDL 和 Java enum 对齐
5. **API URL 复数名词 + 连字符** — 不是驼峰，不用动词
6. **前后端错误码对齐** — 前端 `http.ts` 的拦截器依赖约定错误码
