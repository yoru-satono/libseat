# LibSeat 后端详细设计文档

| 项目 | 内容 |
|------|------|
| 文档编号 | Company-LibSeat-DEV-DD V1.0 |
| 编写日期 | 2026-05-10 |
| 版本状态 | 草稿 |
| 参考文件 | 产品需求规格说明书 V0.1、用户需求说明书 V0.1、api-spec.md |

---

## 目录

- [0. 文档说明](#0-文档说明)
- [1. 项目概述](#1-项目概述)
- [2. 整体架构](#2-整体架构)
- [3. 数据模型设计](#3-数据模型设计)
- [4. API 接口设计](#4-api-接口设计)
- [5. 业务规则设计](#5-业务规则设计)
- [6. 安全设计](#6-安全设计)
- [7. 错误码规范](#7-错误码规范)
- [8. 定时任务设计](#8-定时任务设计)
- [9. 通知设计](#9-通知设计)
- [10. 设计范围与边界说明](#10-设计范围与边界说明)

---

## 0. 文档说明

### 0.1 编写目的

本文档是 LibSeat 图书馆座位预约系统后端服务的详细设计文档，面向系统开发人员，描述后端各模块的设计方案，包括数据模型、接口规范、业务规则、安全机制和部署约定。

### 0.2 适用范围

本文档覆盖 libseat-server 后端服务的全部设计内容，不涉及前端实现和基础设施运维。

### 0.3 参考文件

| 文档 | 编号 |
|------|------|
| 产品需求规格说明书 | Company-LibSeat-REQ-SRS V0.1 |
| 用户需求说明书 | Company-LibSeat-RD-UR V0.1 |
| API 规范 | api-spec.md |

---

## 1. 项目概述

### 1.1 系统目标

LibSeat 是一套面向高校图书馆的座位预约管理系统后端服务，支持学生和教师在线预约座位，并提供图书馆工作人员进行统一管理的后台能力。核心目标：

- 提供精准的座位实时状态查询（可用/已预约/不可用）
- 实现完整的预约生命周期管理（预约→签到→续约→完成/爽约/取消）
- 通过等待队列合理分配被释放的座位资源
- 为管理员提供用户权限、系统规则、审计日志的全面管控能力

### 1.2 技术选型

| 技术 | 选型 | 说明 |
|------|------|------|
| 框架 | Spring Boot 4.0.6 | Web + JPA + Security + Validation + Mail |
| 语言 | Java 25 | |
| 数据库 | PostgreSQL 17 | 原生 ENUM 类型、JSONB、UUID |
| 认证 | Spring Security + jjwt 0.12.6 | JWT 双 Token 无状态认证 |
| ORM | Spring Data JPA + Hibernate | ddl-auto=none，Schema 手动管理 |
| JSON | Jackson 3.x（tools.jackson） | Spring Boot 4.x 内置版本 |
| 测试 | JUnit 5 + Mockito + Testcontainers | 单元测试 + Repository 集成测试 |

---

## 2. 整体架构

### 2.1 分层架构

```
┌─────────────────────────────────────────┐
│              客户端 / 前端               │
└──────────────────┬──────────────────────┘
                   │ HTTP/HTTPS
┌──────────────────▼──────────────────────┐
│         Spring Security 过滤链           │
│  JwtAuthenticationFilter → 权限校验      │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│           Controller 层（8个）           │
│  处理请求绑定、参数校验、响应封装          │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│            Service 层（10个）            │
│  核心业务逻辑、事务管理、异常抛出          │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          Repository 层（10个）           │
│  Spring Data JPA，部分自定义 JPQL        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│           PostgreSQL 17                  │
│  10张表，原生 ENUM，JSONB，UUID PK       │
└─────────────────────────────────────────┘
```

### 2.2 请求生命周期

```
请求到达
  → JwtAuthenticationFilter（解析并验证 Bearer Token，写入 SecurityContext）
  → SecurityFilterChain 权限判断
    → 若未认证且需要认证 → AuthenticationEntryPoint → 返回 {"code":"A0100"}
    → 若权限不足 → AccessDeniedHandler → 返回 {"code":"A0301"}
  → Controller（@Valid 参数校验 → 若失败抛 MethodArgumentNotValidException）
  → GlobalExceptionHandler 统一异常处理
    → BusinessException → 对应错误码
    → MethodArgumentNotValidException → A0400
    → 其他异常 → C0001
  → Service（业务逻辑）
  → Repository（数据访问）
  → 返回 Result<T> 统一包装
```

### 2.3 认证架构（JWT 双 Token）

- **Access Token**：有效期 2 小时，携带用户 ID 和角色，用于业务请求鉴权
- **Refresh Token**：有效期 7 天，仅携带用户 ID，用于换取新 Token 对
- 请求头格式：`Authorization: Bearer <accessToken>`
- Access Token 过期（A0201）→ 客户端调 `/v1/auth/refresh` 换取新 Token 对
- Refresh Token 过期（A0202）→ 客户端重新登录

### 2.4 统一响应格式

所有接口 HTTP 状态码固定返回 `200`，业务结果通过响应体 `code` 字段判断：

```json
{
  "code": "00000",
  "message": "success",
  "data": { ... },
  "timestamp": "2026-05-10T13:00:00+08:00"
}
```

---

## 3. 数据模型设计

### 3.1 实体关系概览

```
Library  ──< Seat  ──< Reservation >── User
   │                       │
   └── SystemRules         ├── Waitlist
                           └── Notification

User ──< EmailToken
User ──< UserChangeRequest >── User (handledBy)
User ──< AuditLog (admin)
Reservation ──< Reservation (parent, 续约链)
```

### 3.2 实体详细设计

#### 3.2.1 Library（图书馆）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| library_id | UUID | PK, NOT NULL | 自动生成 |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 图书馆名称 |
| address | VARCHAR(255) | | 地址 |
| logo_url | VARCHAR(500) | | Logo 图片链接 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | 自动填充 |

#### 3.2.2 User（用户）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | UUID | PK, NOT NULL | 自动生成 |
| user_no | VARCHAR(30) | NOT NULL, UNIQUE | 学号/工号 |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt 哈希 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 登录邮箱 |
| pending_email | VARCHAR(100) | | 待确认的新邮箱 |
| phone | VARCHAR(20) | | 手机号 |
| department | VARCHAR(100) | | 院系/部门 |
| role | user_role | NOT NULL | STUDENT / TEACHER / ADMIN |
| status | user_status | NOT NULL | INACTIVE / ACTIVE / LOCKED / SUSPENDED |
| failed_login_count | SMALLINT | NOT NULL, DEFAULT 0 | 连续失败登录次数 |
| locked_until | TIMESTAMPTZ | | 账号锁定到期时间 |
| suspended_until | TIMESTAMPTZ | | 预约暂停到期时间 |
| no_show_count | SMALLINT | NOT NULL, DEFAULT 0 | 累计爽约次数 |
| email_verified_at | TIMESTAMPTZ | | 邮箱验证时间 |
| last_login_at | TIMESTAMPTZ | | 最后登录时间 |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |
| updated_at | TIMESTAMPTZ | NOT NULL | 触发器自动更新 |

**枚举值：**
- `user_role`：STUDENT、TEACHER、ADMIN
- `user_status`：INACTIVE（未激活）、ACTIVE（正常）、LOCKED（登录锁定）、SUSPENDED（预约暂停）

#### 3.2.3 Seat（座位）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| seat_id | UUID | PK, NOT NULL | 自动生成 |
| library_id | UUID | FK → libraries, NOT NULL | 所属图书馆 |
| seat_no | VARCHAR(20) | NOT NULL | 座位编号（如 3F-001） |
| floor | SMALLINT | NOT NULL | 楼层 |
| area | seat_area | NOT NULL | QUIET / DISCUSSION / COMPUTER |
| has_computer | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否有电脑 |
| has_power | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否有电源插座 |
| has_window | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否靠窗 |
| status | seat_status | NOT NULL, DEFAULT 'AVAILABLE' | AVAILABLE / UNAVAILABLE |
| pos_x | NUMERIC(8,2) | | 平面图 X 坐标 |
| pos_y | NUMERIC(8,2) | | 平面图 Y 坐标 |
| qr_token | CHAR(64) | NOT NULL, UNIQUE | 座位 QR 码令牌（签到验证用） |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |
| updated_at | TIMESTAMPTZ | NOT NULL | 触发器自动更新 |

**唯一约束**：`(library_id, seat_no)`

#### 3.2.4 Reservation（预约）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| reservation_id | UUID | PK, NOT NULL | 自动生成 |
| user_id | UUID | FK → users, NOT NULL | 预约用户 |
| seat_id | UUID | FK → seats, NOT NULL | 预约座位 |
| date | DATE | NOT NULL | 预约日期 |
| start_time | TIME | NOT NULL | 开始时间 |
| end_time | TIME | NOT NULL | 结束时间 |
| status | reservation_status | NOT NULL | 见下方枚举 |
| parent_id | UUID | FK → reservations（自引用） | 续约源记录（续约链） |
| checkin_at | TIMESTAMPTZ | | 签到时间 |
| cancelled_at | TIMESTAMPTZ | | 取消时间 |
| cancel_reason | TEXT | | 取消原因 |
| completed_at | TIMESTAMPTZ | | 完成时间 |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |
| updated_at | TIMESTAMPTZ | NOT NULL | 触发器自动更新 |

**枚举值（reservation_status）：**

| 状态 | 含义 | 触发条件 |
|------|------|---------|
| ACTIVE | 已预约，等待签到 | 创建预约 |
| CHECKED_IN | 已签到 | 扫码签到成功 |
| IN_USE | 使用中 | 预留状态（扩展用） |
| COMPLETED | 已完成 | 定时任务：结束时间已过 |
| CANCELLED | 已取消 | 用户主动取消 |
| NO_SHOW | 爽约 | 定时任务：签到窗口关闭且未签到 |

#### 3.2.5 Waitlist（等待队列）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| waitlist_id | UUID | PK, NOT NULL | 自动生成 |
| user_id | UUID | FK → users, NOT NULL | |
| seat_id | UUID | FK → seats, NOT NULL | |
| date | DATE | NOT NULL | |
| start_time | TIME | NOT NULL | |
| end_time | TIME | NOT NULL | |
| status | waitlist_status | NOT NULL, DEFAULT 'WAITING' | WAITING / NOTIFIED / EXPIRED / CONVERTED |
| notified_at | TIMESTAMPTZ | | 通知用户的时间 |
| expires_at | TIMESTAMPTZ | NOT NULL | 等待过期时间（设为该时段 startTime） |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |

#### 3.2.6 Notification（站内通知）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| notification_id | UUID | PK, NOT NULL | 自动生成 |
| user_id | UUID | FK → users, NOT NULL | 接收用户 |
| type | notification_type | NOT NULL | 见第 9 章 |
| title | VARCHAR(100) | NOT NULL | 通知标题 |
| content | TEXT | NOT NULL | 通知正文 |
| related_id | UUID | | 关联对象 ID（如 reservation_id） |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否已读 |
| read_at | TIMESTAMPTZ | | 标记已读时间 |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |

#### 3.2.7 EmailToken（邮件令牌）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| token_id | UUID | PK, NOT NULL | 自动生成 |
| user_id | UUID | FK → users, NOT NULL | |
| token | VARCHAR(128) | NOT NULL, UNIQUE | 随机安全令牌 |
| type | token_type | NOT NULL | ACTIVATION / PASSWORD_RESET / EMAIL_CHANGE |
| expires_at | TIMESTAMPTZ | NOT NULL | 过期时间（默认 24 小时后） |
| used_at | TIMESTAMPTZ | | 使用时间（使用后不可复用） |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |

#### 3.2.8 UserChangeRequest（关键字段修改申请）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| request_id | UUID | PK, NOT NULL | 自动生成 |
| user_id | UUID | FK → users, NOT NULL | 申请用户 |
| field_name | VARCHAR(50) | NOT NULL | 申请修改的字段（userNo / realName / department） |
| old_value | VARCHAR(200) | NOT NULL | 变更前的值 |
| new_value | VARCHAR(200) | NOT NULL | 变更后的值 |
| status | change_request_status | NOT NULL, DEFAULT 'PENDING' | PENDING / APPROVED / REJECTED |
| handled_by | UUID | FK → users | 审核管理员 |
| handle_note | TEXT | | 审核说明 |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |
| handled_at | TIMESTAMPTZ | | 审核时间 |

**约束**：同一用户同一字段只能有一条 PENDING 申请。

#### 3.2.9 SystemRules（系统规则）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| rule_id | BIGSERIAL | PK | 自增 |
| library_id | UUID | FK → libraries, UNIQUE | NULL 表示全局规则 |
| open_time_start | TIME | NOT NULL | 开馆时间，默认 07:00 |
| open_time_end | TIME | NOT NULL | 闭馆时间，默认 22:00 |
| advance_days_max | SMALLINT | NOT NULL | 最大提前预约天数，默认 7 |
| single_min_minutes | SMALLINT | NOT NULL | 单次最短时长（分钟），默认 30 |
| single_max_hours | SMALLINT | NOT NULL | 单次最长时长（小时），默认 4 |
| daily_max_hours | SMALLINT | NOT NULL | 每日最长累计时长（小时），默认 8 |
| checkin_early_minutes | SMALLINT | NOT NULL | 签到可提前分钟数，默认 10 |
| checkin_late_minutes | SMALLINT | NOT NULL | 签到可延迟分钟数，默认 15 |
| no_show_threshold | SMALLINT | NOT NULL | 爽约次数阈值，默认 3 |
| suspend_days | SMALLINT | NOT NULL | 爽约暂停天数，默认 7 |
| updated_by | UUID | FK → users | 最后修改的管理员 |
| updated_at | TIMESTAMPTZ | NOT NULL | 自动填充 |

**规则优先级**：查询规则时优先使用图书馆级规则（library_id 匹配），不存在则回退到全局规则（library_id IS NULL）。

#### 3.2.10 AuditLog（审计日志）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| log_id | BIGSERIAL | PK | 自增 |
| admin_id | UUID | FK → users, NOT NULL | 执行操作的管理员 |
| action_type | VARCHAR(50) | NOT NULL | 操作类型（UPDATE_USER / HANDLE_CHANGE_REQUEST / UPDATE_SYSTEM_RULES） |
| target_type | VARCHAR(50) | NOT NULL | 目标对象类型（user / change_request / rule） |
| target_id | VARCHAR(100) | NOT NULL | 目标对象 ID |
| detail | JSONB | | 变更前后快照 |
| ip_address | VARCHAR(45) | | 操作 IP（IPv4/IPv6） |
| created_at | TIMESTAMPTZ | NOT NULL | 自动填充 |

### 3.3 关键设计决策

**决策 1：全局规则与图书馆级规则共存**
`system_rules.library_id = NULL` 表示全局默认规则；非 NULL 表示该图书馆的覆盖规则。Service 层查询时优先取图书馆级规则，不存在则使用全局规则。

**决策 2：续约链（parent_id）**
续约时不修改原预约，而是新建一条预约记录，通过 `parent_id` 指向被续约的记录，形成链式结构，完整保留历史记录。

**决策 3：PostgreSQL 原生枚举**
所有枚举字段使用 PostgreSQL `CREATE TYPE ... AS ENUM` 定义，枚举值统一大写（与 Java enum.name() 对齐）。

**决策 4：QR Token 签到验证**
每个座位有唯一 `qr_token`（64 字符随机串），签到时用户扫描座位 QR 码提交 token，后端比对验证，避免代签到。

---

## 4. API 接口设计

### 4.0 通用约定

**Base URL**：`/api/v1`（`server.servlet.context-path=/api` + Controller 前缀 `/v1`）

**时间格式**：ISO 8601 带时区，如 `2026-05-10T09:00:00+08:00`；日期为 `yyyy-MM-dd`；时间为 `HH:mm:ss`

**分页参数**：
- `page`：从 1 开始，默认 1
- `pageSize`：默认 20，最大 100

**分页响应**（`PageResult<T>`）：
```json
{
  "items": [...],
  "total": 100,
  "page": 1,
  "pageSize": 20,
  "totalPages": 5
}
```

---

### 4.1 认证模块（/v1/auth）

所有认证接口无需登录即可访问。

#### POST /v1/auth/register — 用户注册

**请求体**：
```json
{
  "userNo": "S20240001",
  "realName": "张三",
  "password": "pass123456",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "department": "计算机学院"
}
```

**校验规则**：
- `userNo`：必填，最长 30 字符
- `realName`：必填，最长 50 字符
- `password`：必填，8-100 字符，必须同时包含字母和数字（`^(?=.*[A-Za-z])(?=.*\d).+$`）
- `email`：必填，合法邮箱格式

**成功响应**：`{"code":"00000","data":null}`（同时发送激活邮件）

**错误码**：A0400（参数校验失败）、A0500（学号重复）、A0501（邮箱重复）

---

#### POST /v1/auth/activate — 激活账号

```json
{ "token": "<邮件中的激活令牌>" }
```

**成功响应**：`{"code":"00000","data":null}`，账号状态从 INACTIVE 变为 ACTIVE

**错误码**：A0202（令牌无效）、A0201（令牌已过期）

---

#### POST /v1/auth/login — 用户登录

```json
{ "userNo": "S20240001", "password": "pass123456" }
```

**响应**：
```json
{
  "code": "00000",
  "data": {
    "accessToken": "<JWT>",
    "refreshToken": "<JWT>",
    "expiresIn": 7200
  }
}
```

**错误码**：A0600（账号或密码错误）、A0601（账号锁定）、A0603（账号未激活）

---

#### POST /v1/auth/logout — 登出

**认证**：需要登录。无请求体，客户端丢弃本地 Token 即可，服务端无状态。

---

#### POST /v1/auth/refresh — 刷新 Token

```json
{ "refreshToken": "<Refresh Token>" }
```

**响应**：返回新的 Token 对（与登录响应结构相同）

**错误码**：A0202（令牌无效）、A0201（令牌已过期，需重新登录）

---

#### POST /v1/auth/password/reset-request — 忘记密码

```json
{ "email": "zhangsan@example.com" }
```

发送密码重置邮件，无论邮箱是否注册均返回成功（防止枚举用户）。

---

#### POST /v1/auth/password/reset — 重置密码

```json
{ "token": "<邮件中的重置令牌>", "newPassword": "newpass456" }
```

---

#### POST /v1/auth/email/confirm — 确认邮箱变更

```json
{ "token": "<邮件中的确认令牌>" }
```

**错误码**：A0202（令牌无效或 pendingEmail 为空）、A0201（令牌已过期）

---

### 4.2 用户模块（/v1/users/me）

所有接口需要登录。

#### GET /v1/users/me — 获取个人资料

**响应 data**：
```json
{
  "id": "uuid",
  "userNo": "S20240001",
  "realName": "张三",
  "email": "zhangsan@example.com",
  "phone": "138...",
  "department": "计算机学院",
  "role": "STUDENT",
  "status": "ACTIVE",
  "noShowCount": 0,
  "lastLoginAt": "2026-05-10T09:00:00+08:00",
  "createdAt": "2026-01-01T00:00:00+08:00"
}
```

---

#### PATCH /v1/users/me — 修改个人资料

**请求体（字段均可选）**：
```json
{
  "email": "new@example.com",
  "phone": "13900139000",
  "oldPassword": "旧密码（修改密码时必填）",
  "newPassword": "新密码（须满足强度要求）"
}
```

**逻辑**：
- 修改邮箱：写入 `pending_email`，发送确认邮件；不立即生效
- 修改手机：直接更新
- 修改密码：需同时提供 `oldPassword`（验证后更新）

---

#### POST /v1/users/me/change-requests — 申请修改关键信息

```json
{
  "fieldName": "realName",
  "newValue": "李四"
}
```

`fieldName` 可选值：`userNo`、`realName`、`department`

**约束**：同一用户同一字段只能有一条 PENDING 状态的申请

---

#### GET /v1/users/me/change-requests — 查询我的修改申请

**参数**：`page`、`pageSize`

**响应**：分页列表，含每条申请的状态和审核结果

---

### 4.3 图书馆模块（/v1/libraries）

公开端点（无需登录）。

#### GET /v1/libraries — 获取图书馆列表

**响应**：图书馆列表（非分页，数量有限）

```json
[
  { "id": "uuid", "name": "总馆", "address": "校园路1号", "logoUrl": null, "createdAt": "..." }
]
```

#### GET /v1/libraries/{id} — 获取图书馆详情

**错误码**：A0410（图书馆不存在）

---

### 4.4 座位模块（/v1/seats）

GET 端点公开；POST/PUT/DELETE 需要登录（建议加 ADMIN 权限）。

#### GET /v1/seats — 查询座位列表

**查询参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| floor | Short | 按楼层过滤 |
| area | SeatArea | QUIET / DISCUSSION / COMPUTER |
| hasComputer | Boolean | 是否有电脑 |
| hasPower | Boolean | 是否有电源 |
| hasWindow | Boolean | 是否靠窗 |
| date | LocalDate | 配合时间段过滤，查询该时段可用座位 |
| startTime | LocalTime | 格式 HH:mm |
| endTime | LocalTime | 格式 HH:mm |
| page | int | 默认 1 |
| pageSize | int | 默认 20 |

**响应 data**：`PageResult<SeatResponse>`

```json
{
  "id": "uuid", "libraryId": "uuid", "libraryName": "总馆",
  "seatNo": "3F-001", "floor": 3, "area": "QUIET",
  "hasComputer": false, "hasPower": true, "hasWindow": true,
  "status": "AVAILABLE", "posX": 12.5, "posY": 8.0
}
```

#### GET /v1/seats/{seatId} — 查询单个座位详情

#### POST /v1/seats — 创建座位（需 ADMIN）

**请求体**：
```json
{
  "libraryId": "uuid",
  "seatNo": "3F-001",
  "floor": 3,
  "area": "QUIET",
  "hasComputer": false,
  "hasPower": true,
  "hasWindow": false,
  "posX": 12.5,
  "posY": 8.0
}
```

#### PUT /v1/seats/{seatId} — 更新座位信息（需 ADMIN）

#### DELETE /v1/seats/{seatId} — 删除座位（需 ADMIN）

---

### 4.5 预约模块（/v1/reservations）

所有接口需要登录。

#### GET /v1/reservations — 我的预约列表

**查询参数**：`status`（ReservationStatus）、`dateFrom`、`dateTo`、`page`、`pageSize`

**响应**：`PageResult<ReservationResponse>`，按创建时间降序

---

#### GET /v1/reservations/{reservationId} — 预约详情

**错误码**：A0410（不存在）、A0301（非本人预约）

---

#### POST /v1/reservations — 创建预约

**请求体**：
```json
{
  "seatId": "uuid",
  "date": "2026-05-11",
  "startTime": "09:00",
  "endTime": "11:00"
}
```

**业务校验顺序**（见第 5 章）：
1. 用户账号状态检查（INACTIVE / SUSPENDED）
2. 座位状态检查（AVAILABLE）
3. 时间合法性（startTime < endTime）
4. 提前天数校验（≤ advanceDaysMax）
5. 开馆时间校验（在 openTimeStart ~ openTimeEnd 之内）
6. 单次时长校验（≥ singleMinMinutes，≤ singleMaxHours × 60）
7. 每日累计时长校验（usedMinutes + newMinutes ≤ dailyMaxHours × 60）
8. 冲突检测（同座位同时段是否有 ACTIVE/CHECKED_IN/IN_USE 预约）

**成功**：返回预约详情，同时创建 `RESERVATION_SUCCESS` 通知

---

#### DELETE /v1/reservations/{reservationId} — 取消预约

**请求体**（可选）：
```json
{ "cancelReason": "临时有事" }
```

**业务规则**：
- 只能取消自己的预约（否则 A0301）
- 只有 ACTIVE 状态可取消（已签到等状态不可取消，B0300）
- 预约开始时间前 30 分钟内不可取消（B0301）

**成功**：预约状态变为 CANCELLED，`cancelReason` 写入记录，自动触发 `RESERVATION_CANCELLED` 通知，并通知等待队列下一位用户

---

#### POST /v1/reservations/{reservationId}/checkin — 签到

**请求体**：`{ "qrToken": "<座位QR码>" }`

**业务规则**：
- 只能为自己的预约签到
- 只有 ACTIVE 状态可签到
- QR Token 必须与座位记录匹配
- 当前时间必须在签到窗口内：`[startTime - checkinEarlyMinutes, startTime + checkinLateMinutes]`

---

#### POST /v1/reservations/{reservationId}/renew — 续约

**请求体**：`{ "newEndTime": "13:00" }`

**业务规则**：
- 只能为自己的预约续约
- 状态必须为 ACTIVE / CHECKED_IN / IN_USE
- 当前时间必须在续约窗口内（距结束时间 ≤ 15 分钟）
- `newEndTime` 必须晚于当前 `endTime`
- 新时段（`endTime` ~ `newEndTime`）不能与其他预约冲突

**成功**：新建续约预约记录（parent 指向本条），返回新预约详情，同时触发 `RENEWAL_SUCCESS` 通知

---

#### GET /v1/reservations/export — 导出预约历史

**查询参数**：`status`（可选）、`dateFrom`（可选）、`dateTo`（可选）

**业务规则**：
- 需登录，仅导出当前用户的记录
- 导出条数上限 5 000 条，超出返回 A0420
- 返回 `.xlsx` 文件（`Content-Disposition: attachment`）

**导出列**：序号、图书馆、座位号、楼层、区域、预约日期、开始时间、结束时间、时长（分钟）、状态、签到时间、取消时间、取消原因、创建时间

---

### 4.6 等待队列模块（/v1/waitlists）

所有接口需要登录。

#### POST /v1/waitlists — 加入等待队列

**请求体**：
```json
{
  "seatId": "uuid",
  "date": "2026-05-11",
  "startTime": "09:00",
  "endTime": "11:00"
}
```

**业务校验**：
- 用户账号正常（非 INACTIVE / SUSPENDED）
- 日期不能是过去日期
- startTime 必须早于 endTime
- 同一用户、同座位、同时段不能有 WAITING / NOTIFIED 状态的等待记录（B0600）

**expiresAt** 自动设置为该时段的开始时间（时间到了则等待无意义）

---

#### GET /v1/waitlists — 我的等待队列列表

**查询参数**：`status`（WaitlistStatus，可选）、`page`、`pageSize`

---

#### DELETE /v1/waitlists/{id} — 取消等待

**业务规则**：只有 WAITING / NOTIFIED 状态可取消（状态改为 EXPIRED，B0601）

---

### 4.7 通知模块（/v1/notifications）

所有接口需要登录。

#### GET /v1/notifications — 通知列表

**查询参数**：`isRead`（Boolean，可选，`false` = 仅未读，`true` = 仅已读，不传 = 全部）、`page`、`pageSize`

---

#### GET /v1/notifications/unread-count — 未读数量

**响应**：`{ "count": 3 }`

---

#### PATCH /v1/notifications/{id}/read — 标记单条已读

**错误码**：A0410（不存在）、A0301（非本人通知）

---

#### PATCH /v1/notifications/read-all — 全部标记已读

---

#### DELETE /v1/notifications/{id} — 删除通知

---

### 4.8 管理后台（/v1/admin）

所有接口需要 ADMIN 角色。

#### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/users | 用户列表（支持 role / status 过滤） |
| GET | /v1/admin/users/{userId} | 用户详情 |
| PATCH | /v1/admin/users/{userId} | 修改用户（角色/状态/密码重置/爽约清零） |

**PATCH 请求体（字段均可选）**：
```json
{
  "role": "ADMIN",
  "status": "ACTIVE",
  "newPassword": "重置后的密码",
  "resetNoShowCount": true
}
```

**修改 status 为 ACTIVE 时**：同步清除 `lockedUntil` 和 `failedLoginCount`，相当于解锁

所有修改自动写入审计日志。

---

#### 预约管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/reservations | 全平台预约列表（支持 status / dateFrom / dateTo 过滤） |
| GET | /v1/admin/reservations/{reservationId} | 单条预约详情 |
| DELETE | /v1/admin/reservations/{reservationId} | 管理员取消预约（仅限 ACTIVE 状态），自动写入审计日志 |
| GET | /v1/admin/reservations/export | 导出预约记录（支持 userId / status / dateFrom / dateTo 过滤），上限 5 000 条，返回 .xlsx 文件 |

**响应字段**比用户侧多：`userId`、`userNo`、`realName`、`floor`；导出文件额外包含用户学号/工号和姓名列

---

#### 信息修改申请

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/change-requests | 待审核申请列表 |
| PATCH | /v1/admin/change-requests/{requestId} | 审核（批准/拒绝） |

**审核请求体**：
```json
{ "action": "APPROVED", "handleNote": "已核实" }
```

审核批准时自动将 `newValue` 写入用户对应字段，同时写入审计日志。

---

#### 图书馆管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/libraries | 图书馆列表 |
| GET | /v1/admin/libraries/{id} | 图书馆详情 |
| POST | /v1/admin/libraries | 创建图书馆 |
| PUT | /v1/admin/libraries/{id} | 更新图书馆 |
| DELETE | /v1/admin/libraries/{id} | 删除图书馆（有关联座位时拒绝） |

---

#### 系统规则

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/system-rules | 获取全局系统规则 |
| PUT | /v1/admin/system-rules | 更新全局系统规则，自动写审计日志 |

**规则字段**：openTimeStart / openTimeEnd / advanceDaysMax / singleMinMinutes / singleMaxHours / dailyMaxHours / checkinEarlyMinutes / checkinLateMinutes / noShowThreshold / suspendDays

---

#### 审计日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/admin/audit-logs | 审计日志列表（支持 adminId / targetType / targetId / dateFrom / dateTo 过滤） |

---

## 5. 业务规则设计

### 5.1 账号生命周期

```
注册
  → INACTIVE（账号创建，未激活）
    → 点击激活链接 → ACTIVE（正常）
      → 连续登录失败 5 次 → LOCKED（临时锁定 30 分钟）
        → 30 分钟后自动解锁 / 管理员手动解锁 → ACTIVE
      → 爽约次数 ≥ noShowThreshold → SUSPENDED（预约暂停）
        → suspendDays 天后到期自动恢复 / 管理员手动恢复 → ACTIVE
```

### 5.2 预约业务规则

| 规则 | 默认值 | 配置项 |
|------|--------|--------|
| 最大提前预约天数 | 7 天 | advanceDaysMax |
| 单次最短时长 | 30 分钟 | singleMinMinutes |
| 单次最长时长 | 4 小时 | singleMaxHours |
| 每日最大累计时长 | 8 小时 | dailyMaxHours |
| 可预约时间范围 | 07:00–22:00 | openTimeStart / openTimeEnd |
| 取消预约截止提前时间 | 30 分钟 | 硬编码（后续可迁移至 SystemRules） |

**冲突检测逻辑**：若同一座位、同日期，存在状态为 ACTIVE / CHECKED_IN / IN_USE 的预约，且时间段有重叠，则拒绝新预约（B0200）。

### 5.3 签到机制

- **签到窗口**：`[startTime - checkinEarlyMinutes, startTime + checkinLateMinutes]`（默认提前 10 分钟到迟到 15 分钟）
- **验证方式**：扫描座位 QR 码，提交 `qrToken` 与座位记录比对
- **窗口关闭后**：由定时任务（每 5 分钟执行）将 ACTIVE 状态的过期预约标记为 NO_SHOW

### 5.4 续约机制

- **续约窗口**：距当前预约结束时间不超过 15 分钟时可续约
- **续约操作**：新建预约记录（`startTime = 当前 endTime`，`parent = 当前预约`），原记录不变
- **冲突检测**：对新时段（`endTime` ~ `newEndTime`）执行同样的冲突检测

### 5.5 爽约与账号暂停机制

1. 定时任务（每 5 分钟）扫描签到窗口已关闭且状态仍为 ACTIVE 的预约
2. 将其标记为 NO_SHOW，用户 `noShowCount + 1`
3. 若 `noShowCount >= noShowThreshold`（默认 3）：
   - 设置 `suspendedUntil = now + suspendDays 天`
   - 重置 `noShowCount = 0`
4. 同时通知等待队列中该时段的首位用户

### 5.6 等待队列自动通知流程

```
预约被取消（cancelReservation）或标记为爽约（定时任务）
  → WaitlistService.notifyNextInQueue(seatId, date, startTime, endTime)
    → 查询该时段 WAITING 状态等待记录，按 createdAt 升序取第一条
    → 将该记录状态改为 NOTIFIED，记录 notifiedAt
    → 创建 WAITLIST_AVAILABLE 通知发送给等待用户
```

### 5.7 邮箱验证流程

**注册激活**：注册 → 发送 ACTIVATION 令牌邮件 → 用户点击链接调 `/v1/auth/activate` → 账号激活

**密码重置**：调 `/v1/auth/password/reset-request` → 发送 PASSWORD_RESET 令牌邮件 → 用户调 `/v1/auth/password/reset` → 更新密码

**邮箱变更**：用户在 PATCH /v1/users/me 中传新邮箱 → 写入 `pending_email` → 发送 EMAIL_CHANGE 令牌邮件 → 用户调 `/v1/auth/email/confirm` → 将 `pending_email` 写入 `email`，清空 `pending_email`

---

## 6. 安全设计

### 6.1 接口权限矩阵

| 路径 | 匿名 | 登录用户 | ADMIN |
|------|------|---------|-------|
| GET /v1/libraries/\*\* | ✅ | ✅ | ✅ |
| GET /v1/seats/\*\* | ✅ | ✅ | ✅ |
| /v1/auth/register、login、activate 等 | ✅ | ✅ | ✅ |
| /v1/auth/email/confirm | ✅ | ✅ | ✅ |
| /v1/users/me/\*\* | ❌ | ✅ | ✅ |
| /v1/reservations/\*\* | ❌ | ✅ | ✅ |
| /v1/waitlists/\*\* | ❌ | ✅ | ✅ |
| /v1/notifications/\*\* | ❌ | ✅ | ✅ |
| POST/PUT/DELETE /v1/seats/\*\* | ❌ | ✅（建议限 ADMIN） | ✅ |
| /v1/admin/\*\* | ❌ | ❌ | ✅ |

### 6.2 JWT 认证流程

```
每次请求
  → JwtAuthenticationFilter
    → 读取 Authorization: Bearer <token>
    → JwtService.parse(token) 验证签名和有效期
    → 若 Token 过期：存入 request attribute (ATTR_JWT_ERROR = A0201)
    → 若格式无效：存入 request attribute (ATTR_JWT_ERROR = A0202)
    → 若有效：从 token 中提取 userId，查询 User 实体，写入 SecurityContext
  → AuthenticationEntryPoint
    → 读取 request attribute 中的错误码，返回对应 JSON 响应
```

### 6.3 密码安全

- **存储**：BCrypt 哈希（Spring Security BCryptPasswordEncoder）
- **强度要求**：最少 8 字符，必须同时包含字母和数字
- **传输**：仅在 HTTPS 下传输（运维配置）

### 6.4 账号锁定机制

**临时锁定（密码连续错误）**

- 连续登录失败 5 次 → `lockedUntil = now + 30 分钟`，`failedLoginCount = 0`
- 锁定期间所有登录尝试均返回 A0601
- 登录成功后 `failedLoginCount` 重置为 0，`lockedUntil` 清空

**状态锁定（管理员操作）**

- `status = LOCKED` 时，不论 `lockedUntil` 是否过期，登录均返回 A0601
- 需由管理员将 `status` 改回 `ACTIVE` 方可解锁

---

## 7. 错误码规范

所有错误码为 5 位字符串，首 2 位表示错误来源：
- `A0xxx`：用户端错误（认证/鉴权/参数/账号状态）
- `B0xxx`：业务规则错误（座位/预约/签到/续约/等待队列）
- `C0xxx`：服务端错误

### 完整错误码表

| 错误码 | 含义 |
|--------|------|
| **00000** | 成功 |
| **A0100** | 未登录 |
| **A0201** | Access Token 已过期 |
| **A0202** | Token 无效 |
| **A0301** | 无权限访问该资源 |
| **A0400** | 请求参数校验失败 |
| **A0401** | 必填参数缺失 |
| **A0402** | 参数格式错误 |
| **A0410** | 资源不存在 |
| **A0420** | 导出数据量超过上限（5000条） |
| **A0500** | 学号/工号已存在 |
| **A0501** | 邮箱已被注册 |
| **A0502** | 图书馆名称已存在 |
| **A0600** | 账号或密码错误 |
| **A0601** | 账号已锁定，请稍后重试 |
| **A0602** | 账号已暂停预约权限 |
| **A0603** | 账号未激活 |
| **B0100** | 座位不可用 |
| **B0200** | 预约时段冲突 |
| **B0201** | 超出最大提前预约天数 |
| **B0202** | 单次预约时长不合规 |
| **B0203** | 预约时间超出图书馆开放时间 |
| **B0210** | 超出每日累计预约时长上限 |
| **B0300** | 预约已开始，不可取消 |
| **B0301** | 预约开始前30分钟内不可取消 |
| **B0400** | 不在签到时间窗口内 |
| **B0500** | 该时段已有人预约，无法续约 |
| **B0501** | 不在续约时间窗口内 |
| **B0600** | 已在该时段等待队列中 |
| **B0601** | 等待记录状态不可取消 |
| **C0001** | 服务器内部错误 |
| **C0002** | 服务暂时不可用 |

---

## 8. 定时任务设计

### 8.1 预约过期扫描

**触发频率**：每 5 分钟（fixedDelay = 300,000ms）

**逻辑**：

```
Step 1 — 爽约标记
  查询 status=ACTIVE 且 date <= today 的所有预约
  对每条记录计算签到窗口结束时间：startTime + checkinLateMinutes
  若当前时间已超过窗口结束时间：
    → 将预约状态改为 NO_SHOW
    → user.noShowCount++
    → 若 noShowCount >= noShowThreshold：
        设置 user.suspendedUntil = now + suspendDays 天
        重置 user.noShowCount = 0
    → 调用 WaitlistService.notifyNextInQueue()

Step 2 — 完成标记
  查询 status IN (CHECKED_IN, IN_USE) 且 date <= today 的所有预约
  对每条记录：若当前时间超过 endTime
    → 将预约状态改为 COMPLETED，记录 completedAt
```

**系统规则加载**：优先使用全局规则（library_id IS NULL），规则项缺失时使用硬编码默认值（lateMinutes=15，threshold=3，suspendDays=7）。

### 8.2 邮件令牌清理

**触发时间**：每天 03:00（cron = `0 0 3 * * *`）

**逻辑**：删除 `expires_at < now AND used_at IS NULL` 的所有邮件令牌

---

## 9. 通知设计

### 9.1 通知类型说明

| 类型 | 含义 |
|------|------|
| RESERVATION_SUCCESS | 预约成功确认 |
| RESERVATION_CANCELLED | 预约已取消 |
| CHECKIN_REMINDER | 签到提醒（计划中，需定时任务） |
| NO_SHOW_WARNING | 爽约警告 |
| ACCOUNT_LOCKED | 账号锁定通知 |
| ACCOUNT_SUSPENDED | 账号预约权限暂停通知 |
| WAITLIST_AVAILABLE | 等待的座位已可预约 |
| RENEWAL_SUCCESS | 续约成功确认 |
| SYSTEM | 系统公告 |

### 9.2 各业务触发点

| 业务操作 | 自动创建通知类型 |
|---------|---------------|
| 预约成功（createReservation） | RESERVATION_SUCCESS |
| 预约取消（cancelReservation） | RESERVATION_CANCELLED |
| 续约成功（renew） | RENEWAL_SUCCESS |
| 等待队列触发（notifyNextInQueue） | WAITLIST_AVAILABLE |
| 爽约标记（定时任务） | NO_SHOW_WARNING |
| 账号锁定（login 连续失败 5 次） | ACCOUNT_LOCKED |
| 账号暂停（爽约超限，定时任务） | ACCOUNT_SUSPENDED |

---

## 10. 设计范围与边界说明

### 10.1 需求覆盖度矩阵

| 需求编号 | 需求描述 | 覆盖情况 |
|---------|---------|---------|
| FR-1.1.1 | 用户注册（含密码强度、激活邮件） | ✅ |
| FR-1.1.2 | 用户登录（含失败锁定） | ✅ |
| FR-1.1.3 | 个人资料修改（含关键信息申请审核、邮箱二次验证） | ✅ |
| FR-1.2.1 | 座位浏览（可视化地图） | ✅ 后端提供 posX/posY 坐标，前端基于坐标渲染地图 |
| FR-1.2.2 | 座位状态查询（多条件过滤） | ✅ |
| FR-1.2.3 | 座位区域管理（Admin CRUD） | ✅ |
| FR-1.3.1 | 座位预约（多维度校验） | ✅ |
| FR-1.3.2 | 预约取消（含 30 分钟截止窗口） | ✅ |
| FR-1.3.3 | 现场签到（QR 扫码 + 时间窗口） | ✅ |
| FR-1.3.4 | 座位续约（15 分钟窗口） | ✅ |
| FR-1.3.5 | 预约历史查询与导出（Excel） | ✅ |
| FR-1.4.1 | 用户权限管理（Admin） | ✅ |
| FR-1.4.2 | 系统规则配置 | ✅ |
| FR-1.4.3 | 数据备份与恢复 | ⬜ 超出应用层范围（见 10.2） |
| — | 等待队列 | ✅（主动补充，提升座位利用率） |
| — | 站内通知（查询/已读/删除） | ✅（主动补充，提升用户体验） |
| — | 审计日志查询 | ✅（Admin 可查） |
| — | 图书馆管理（Admin CRUD） | ✅（主动补充） |

### 10.2 超出设计范围的需求

#### 数据备份与恢复（FR-1.4.3）

该需求属于基础设施运维职责，不在应用层 API 设计范围内。理由如下：

1. **权限边界**：应用进程只需 DML 权限，备份/恢复需数据库级别权限，不应通过 HTTP 端点暴露
2. **性能**：`pg_dump` / `pg_restore` 效率远优于 JPA 层面的数据迁移
3. **完整性**：应用层无法还原数据库结构、索引、序列状态等元数据

**推荐实现方式**：
- 在部署环境中配置 cron 定期执行 `pg_dump`，备份文件存入对象存储
- 生产环境使用云数据库自动快照功能（如 RDS、Cloud SQL）
- 若需要可审计的手动触发入口，可在运维平台（而非业务 API）添加触发钩子

### 10.3 后续迭代建议

| 优先级 | 建议项 |
|--------|--------|
| 中 | 签到提醒（CHECKIN_REMINDER）：在预约开始前若干分钟由定时任务推送，需新增定时任务和可配置的提前提醒时间参数 |
| 中 | 取消截止窗口可配置化：将当前硬编码的 30 分钟改为 SystemRules 中的可配置参数（如新增 `cancelCutoffMinutes`） |
| 低 | 座位可视化地图数据接口：为前端提供按楼层/区域聚合的座位坐标批量查询接口，优化地图渲染性能 |
| 低 | PDF 格式导出：在现有 Excel 导出基础上，引入 OpenPDF 支持 PDF 格式，适合打印存档场景 |
