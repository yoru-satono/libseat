# LibSeat 后端测试方案文档

| 项目 | 内容 |
|------|------|
| 文档编号 | Company-LibSeat-DEV-TP V1.0 |
| 编写日期 | 2026-05-10 |
| 版本状态 | 草稿 |
| 参考文件 | 产品需求规格说明书 V0.1、详细设计文档 V1.0、api-spec.md |

---

## 目录

- [1. 概述](#1-概述)
- [2. 测试范围与分层策略](#2-测试范围与分层策略)
- [3. 测试环境](#3-测试环境)
- [4. 测试数据](#4-测试数据)
- [5. 内部测试（单元 & 集成）](#5-内部测试单元--集成)
- [6. 端到端 API 测试](#6-端到端-api-测试)
- [7. 各模块测试用例](#7-各模块测试用例)
- [8. 已知局限与手动测试项](#8-已知局限与手动测试项)

---

## 1. 概述

### 1.1 编写目的

本文档描述 LibSeat 后端服务的完整测试方案，涵盖测试分层策略、测试环境、测试数据、用例设计及执行方式。供开发人员与质量审核人员参考。

### 1.2 测试目标

| 目标 | 说明 |
|------|------|
| 功能正确性 | 各接口在正常输入下返回预期结果 |
| 鉴权覆盖 | 所有需要认证/权限的接口，未登录返回 A0100，权限不足返回 A0301 |
| 业务规则 | 预约冲突、取消窗口、导出上限等核心规则得到验证 |
| 响应格式 | 所有接口 HTTP 200，成功 `code=00000`，失败返回对应业务错误码 |

---

## 2. 测试范围与分层策略

本项目采用三层测试策略：

```
┌─────────────────────────────────┐
│   端到端 API 测试（外部 HTTP）     │  ← pytest + httpx（本文档重点）
├─────────────────────────────────┤
│   集成测试（Repository 层）       │  ← Testcontainers + 真实 PostgreSQL
├─────────────────────────────────┤
│   单元测试（Service / Controller）│  ← JUnit 5 + Mockito / MockMvc
└─────────────────────────────────┘
```

### 2.1 层级职责

| 层级 | 工具 | 目标 | 数量（当前） |
|------|------|------|-------------|
| Controller 单元测试 | MockMvc + Mockito | 路由、鉴权逻辑、响应格式 | 约 60 个 |
| Service 单元测试 | JUnit 5 + Mockito | 业务规则、异常路径 | 约 80 个 |
| Repository 集成测试 | Testcontainers + PostgreSQL 17 | SQL 正确性、分页、Specification 过滤 | 约 50 个 |
| 端到端 API 测试 | pytest + httpx | 完整请求链路、真实 JWT、跨层联动 | 68 个 |

---

## 3. 测试环境

### 3.1 内部测试环境

| 组件 | 版本 |
|------|------|
| JDK | Java 25 |
| Spring Boot | 4.0.6 |
| Maven | 3.9.15 |
| JUnit | 5（via Spring Boot Test） |
| Testcontainers | 1.21.x（PostgreSQL 17） |

运行命令：

```bash
# 全部内部测试
mvn -f /home/chain/Documents/Workspaces/libseat-server/pom.xml test

# 单个测试类
mvn -f /home/chain/Documents/Workspaces/libseat-server/pom.xml test -Dtest=ReservationServiceTest
```

### 3.2 端到端 API 测试环境

| 组件 | 版本 |
|------|------|
| Python | 3.14 |
| uv | 最新 |
| pytest | ≥9.0 |
| httpx | ≥0.28（同步模式） |
| openpyxl | ≥3.1（Excel 内容验证） |

测试项目位置：`/home/chain/Documents/Workspaces/libseat-server-test/`

**运行前置条件**：

1. 数据库已启动并导入完整种子数据：
   ```bash
   docker compose -f /home/chain/Documents/Workspaces/libseat-server/compose.yml up -d
   ```
2. 应用服务器已启动（监听 `localhost:8080`）：
   ```bash
   mvn -f /home/chain/Documents/Workspaces/libseat-server/pom.xml spring-boot:run
   ```
3. 在测试项目目录执行：
   ```bash
   cd /home/chain/Documents/Workspaces/libseat-server-test
   uv run pytest                          # 全部
   uv run pytest tests/test_auth.py -v   # 单模块
   BASE_URL=http://host:8080/api/v1 uv run pytest  # 自定义地址
   ```

---

## 4. 测试数据

### 4.1 种子数据说明

端到端测试依赖 `sql/init/02_seed.sql` 中的固定种子数据，所有账号密码均为 `password`。测试须在**数据库完整重置并重新导入种子数据**后运行，以保证只读断言的确定性。

完整重置方式（仅限开发环境）：

```sql
TRUNCATE libraries, users, seats, system_rules, reservations,
         waitlists, email_tokens, user_change_requests,
         notifications, audit_logs RESTART IDENTITY CASCADE;
-- 重新导入：
\i sql/init/01_schema.sql
\i sql/init/02_seed.sql
```

### 4.2 测试账号

| 账号编号 | 姓名 | 角色 | 状态 | 用途 |
|---------|------|------|------|------|
| A000001 | 系统管理员 | ADMIN | ACTIVE | 管理员接口测试 |
| S202401001 | 王小明 | STUDENT | ACTIVE | 普通用户主账号（user1） |
| S202401002 | 李小红 | STUDENT | ACTIVE | 普通用户次账号（user2，隔离测试） |
| S202401003 | 陈大勇 | STUDENT | INACTIVE | 验证未激活账号登录拒绝（A0603） |
| S202401004 | 刘小芳 | STUDENT | LOCKED | 验证锁定账号登录拒绝（A0601） |
| S202401005 | 张伟 | STUDENT | SUSPENDED | 验证暂停账号登录拒绝（A0602） |

### 4.3 关键 UUID 速查

| 类别 | 描述 | UUID |
|------|------|------|
| 图书馆 | 中心图书馆 | `a0000000-0000-0000-0000-000000000001` |
| 座位 | 2F-Q-001（QUIET，可用） | `c0000000-0000-0000-0000-000000000004` |
| 座位 | 1F-D-003（DISCUSSION，不可用） | `c0000000-0000-0000-0000-000000000003` |
| 预约 | 王小明，昨天，COMPLETED | `d0000000-0000-0000-0000-000000000002` |
| 通知 | 王小明签到提醒（未读） | `ab000000-0000-0000-0000-000000000002` |
| 变更申请 | 王小明改院系（PENDING） | `ca000000-0000-0000-0000-000000000001` |

### 4.4 图书馆规则（影响预约测试）

| 规则 | 值 |
|------|------|
| 开放时间 | 08:00–21:00 |
| 最长提前预约天数 | 3 天 |
| 单次最短时长 | 30 分钟 |
| 单次最长时长 | 4 小时 |
| 每日最长累计 | 8 小时 |
| 签到窗口 | 开始前 10 分钟至开始后 15 分钟 |

---

## 5. 内部测试（单元 & 集成）

### 5.1 Controller 层测试

使用 `@WebMvcTest` + `MockMvc`，不启动完整容器。所有 Service 依赖通过 `@MockitoBean` 替换。

| 测试类 | 覆盖端点 |
|--------|---------|
| `AuthControllerTest` | POST /auth/login、/refresh、/logout |
| `UserControllerTest` | GET/PATCH /users/me、change-requests |
| `SeatControllerTest` | GET /seats、/seats/{id} |
| `ReservationControllerTest` | GET/POST /reservations、/export、DELETE /{id} |
| `AdminControllerTest` | /admin/users、/admin/reservations、/admin/system-rules、/admin/audit-logs |
| `NotificationControllerTest` | GET/PATCH /notifications |
| `LibraryControllerTest` | GET /libraries |
| `WaitlistControllerTest` | POST/DELETE /waitlists |

验证要点：鉴权（未登录 A0100、权限不足 A0301）、参数校验（A0400）、响应体 `code` 字段、Content-Type（Excel 端点）。

### 5.2 Service 层测试

使用纯 JUnit 5 + Mockito，不启动 Spring 容器。

| 测试类 | 核心验证点 |
|--------|-----------|
| `AuthServiceTest` | 登录锁定逻辑（5 次失败）、token 生成、账号状态校验 |
| `ReservationServiceTest` | 时间冲突检测、取消窗口（30 分钟）、每日上限、续约链 |
| `AdminServiceTest` | 管理员取消、审计日志写入、导出上限（5000 条） |
| `ExcelExportServiceTest` | 表头正确性、行数、枚举中文翻译（安静区、待开始等） |
| `ReservationSchedulerTest` | NO_SHOW 标记、爽约暂停触发 |

### 5.3 Repository 集成测试

通过 `RepositoryTestBase`（Testcontainers PostgreSQL 17）在真实数据库中运行，每个方法在事务内自动回滚。

| 测试类 | 核心验证点 |
|--------|-----------|
| `SeatRepositoryTest` | 按区域/楼层过滤、时间段可用性查询 |
| `ReservationRepositoryTest` | Specification 组合过滤、分页排序 |
| `UserRepositoryTest` | userNo 查找、状态过滤 |
| `NotificationRepositoryTest` | markAllRead 的 `clearAutomatically` 行为 |

---

## 6. 端到端 API 测试

### 6.1 技术选型

| 工具 | 职责 |
|------|------|
| `uv` | 虚拟环境与依赖管理 |
| `httpx`（同步模式） | HTTP 客户端，发送真实请求 |
| `pytest` | 测试框架，fixture 管理 |
| `openpyxl` | 解析 Excel 导出内容，验证表头与行数 |

### 6.2 Fixture 设计

```python
# conftest.py（session 级别，整个测试会话只登录一次）
anon()    # 未认证客户端
admin()   # 以 A000001 登录
user1()   # 以 S202401001 登录（王小明）
user2()   # 以 S202401002 登录（李小红，与 user1 隔离）
```

**隔离原则**：修改性测试（创建→取消预约等）在测试内自行创建数据，不依赖种子预约的特定状态，避免测试顺序依赖。

### 6.3 测试文件组织

| 文件 | 用例数 | 说明 |
|------|--------|------|
| `test_auth.py` | 8 | 登录各状态、刷新 token、登出 |
| `test_users.py` | 7 | 个人信息读写、变更申请列表 |
| `test_seats.py` | 7 | 座位列表过滤、单条查询 |
| `test_reservations.py` | 14 | 预约全流程、冲突、取消、Excel 导出 |
| `test_notifications.py` | 6 | 通知列表、已读标记、未读计数 |
| `test_admin_users.py` | 9 | 用户列表过滤、变更申请审批 |
| `test_admin_reservations.py` | 7 | 管理员查询/取消、Excel 导出内容 |
| `test_admin_system.py` | 10 | 系统规则、图书馆管理、审计日志 |
| **合计** | **68** | |

---

## 7. 各模块测试用例

### 7.1 认证模块（`test_auth.py`）

| 用例 | 输入 | 期望 |
|------|------|------|
| 正常登录 | 有效账号密码 | `code=00000`，含 accessToken + refreshToken |
| 密码错误 | 正确账号 + 错误密码 | `code=A0600` |
| 未激活账号 | S202401003 | `code=A0603` |
| 锁定账号 | S202401004 | `code=A0601` |
| 暂停账号 | S202401005 | `code=A0602` |
| 刷新 token | 有效 refreshToken | `code=00000`，新 accessToken |
| 登出 | 已登录 Bearer token | `code=00000` |
| 缺少字段 | 无 password 字段 | `code` ≠ 00000 |

### 7.2 用户模块（`test_users.py`）

| 用例 | 期望 |
|------|------|
| 未登录访问 /users/me | `code=A0100` |
| 获取个人信息 | `code=00000`，userNo 匹配 |
| 响应字段完整性 | 含 userId/userNo/realName/email/role/status |
| 更新手机号 | `code=00000` |
| 未登录更新 | `code=A0100` |
| 获取变更申请列表 | `code=00000`，含种子 PENDING 申请 |
| 未登录获取申请 | `code=A0100` |

### 7.3 座位模块（`test_seats.py`）

| 用例 | 期望 |
|------|------|
| 未登录访问 /seats | `code=A0100` |
| 按图书馆过滤 | 所有结果属于该 libraryId |
| status=AVAILABLE 过滤 | 不含 UNAVAILABLE 座位 |
| area=QUIET 过滤 | 所有结果 area=QUIET |
| area=COMPUTER 过滤 | 所有结果 area=COMPUTER |
| 按 ID 查询（2F-Q-001） | seatNo=2F-Q-001，floor=2，area=QUIET |
| 查询不存在的 ID | `code` ≠ 00000 |

### 7.4 预约模块（`test_reservations.py`）

| 用例 | 期望 |
|------|------|
| 创建预约（成功） | `code=00000`，status=ACTIVE |
| 未登录创建 | `code=A0100` |
| 缺少必填字段 | `code=A0400` |
| 同座位同时段冲突 | `code=B0201` |
| 获取预约列表 | 分页结构完整（items/total/page/pageSize/totalPages） |
| 未登录获取列表 | `code=A0100` |
| 按 status=COMPLETED 过滤 | 所有结果 status=COMPLETED |
| 按 ID 获取 | `code=00000`，字段完整 |
| 获取他人预约 | 返回业务错误（A0301 或 B0100） |
| 取消预约 | `code=00000` |
| 带取消原因取消 | `code=00000` |
| 重复取消已取消的预约 | `code` ≠ 00000 |
| 未登录导出 | `code=A0100` |
| 导出 Excel | Content-Type 含 spreadsheetml，Content-Disposition 含 attachment |

### 7.5 通知模块（`test_notifications.py`）

| 用例 | 期望 |
|------|------|
| 未登录获取通知 | `code=A0100` |
| 获取通知列表 | `code=00000`，分页结构正确 |
| 仅未读过滤 | 所有结果 isRead=false |
| 未读计数 | `code=00000`，count 为非负整数 |
| 标记单条已读 | `code=00000` |
| 全部标记已读 | `code=00000`，之后 unread count=0 |

### 7.6 管理员-用户管理（`test_admin_users.py`）

| 用例 | 期望 |
|------|------|
| 未登录访问 /admin/users | `code=A0100` |
| 普通用户访问 | `code=A0301` |
| 管理员获取用户列表 | `code=00000`，total ≥ 8 |
| role=STUDENT 过滤 | 所有结果 role=STUDENT |
| status=ACTIVE 过滤 | 所有结果 status=ACTIVE |
| 分页（pageSize=3） | 结果数 ≤ 3 |
| 获取变更申请列表 | `code=00000`，含种子 PENDING 申请 |
| 普通用户获取申请 | `code=A0301` |
| 审批变更申请 | `code=00000` 或已处理的业务错误 |

### 7.7 管理员-预约管理（`test_admin_reservations.py`）

| 用例 | 期望 |
|------|------|
| 普通用户获取预约详情 | `code=A0301` |
| 未登录获取详情 | `code=A0100` |
| 管理员获取预约详情 | `code=00000`，含 userNo/realName 字段 |
| 管理员取消预约 | `code=00000`，预约状态变为 CANCELLED |
| 普通用户取消他人预约 | `code=A0301` |
| 普通用户请求导出 | `code=A0301` |
| 管理员导出 Excel | 表头第 2 列=学号/工号，第 3 列=姓名 |

### 7.8 管理员-系统配置（`test_admin_system.py`）

| 用例 | 期望 |
|------|------|
| 未登录获取系统规则 | `code=A0100` |
| 普通用户获取系统规则 | `code=A0301` |
| 管理员获取系统规则 | `code=00000`，advanceDaysMax=3，开放时间 08:00–21:00 |
| 普通用户获取图书馆列表 | `code=A0301` |
| 管理员获取图书馆列表 | `code=00000`，含"中心图书馆" |
| 按 ID 获取图书馆 | `code=00000`，name=中心图书馆 |
| 普通用户获取审计日志 | `code=A0301` |
| 未登录获取审计日志 | `code=A0100` |
| 管理员获取审计日志 | `code=00000`，total ≥ 2 |
| 审计日志分页 | pageSize=1 时结果数 ≤ 1 |

---

## 8. 已知局限与手动测试项

以下场景难以在自动化测试中可靠复现，需手动验证：

| 场景 | 原因 | 验证方式 |
|------|------|---------|
| 取消窗口（B0301） | 需预约在未来 30 分钟内开始，时序依赖强 | 手动创建临近时段预约后尝试取消 |
| 签到窗口（B0202） | 需预约在当前时段内，时序依赖强 | 手动在开放签到窗口内测试 |
| 定时任务（爽约标记） | `ReservationScheduler` 每 15 分钟执行，无法在 E2E 中直接触发 | 观察超时未签到预约的状态变化 |
| 导出 5000 条上限（A0420） | 需大量真实数据 | 种子数据不足，可通过批量插入后测试 |
| 刷新 token 过期（A0202） | 需等待 7 天 | 修改 JWT 配置缩短有效期后测试 |
| 邮箱验证激活流程 | 依赖外部邮件发送 | 通过数据库直接操作 email_tokens 表验证 |
| 续约（`/renew`） | 需预约处于可续约状态 | 手动在预约临近结束时测试 |
