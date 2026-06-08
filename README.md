# LibSeat — 图书馆座位预约系统

高校图书馆座位预约管理系统，支持座位浏览、预约/签到/签退、续约、等候队列、信息修改审批、管理员后台等功能。

## 项目结构

```
libseat/
├── server/          # 后端 — Spring Boot 4.0.6 · Java 25 · PostgreSQL 17
├── web/             # 前端 — Vue 3 · Vite · Tailwind CSS 4 · Pinia
├── test/            # 端到端 API 测试 — Python · pytest · httpx
├── docs/            # 产品文档（需求规格说明书）
├── docker-compose.yml   # 开发基础设施（DB + Mailpit）
└── .env             # 环境变量（DB 密码、JWT 密钥等）
```

## 功能概览

### 用户端 (Web)
- **座位浏览** — 按图书馆、区域（安静区/讨论区/电脑区）、时间段筛选可用座位
- **预约管理** — 创建预约、签到、签退、取消、续约
- **等候队列** — 满座时加入等候，有空位自动通知转换
- **个人信息** — 修改姓名/院系/联系方式，提交管理员审批
- **通知中心** — 预约成功、签到提醒、爽约警告、账号状态变更

### 管理端 (Web)
- **仪表盘** — 核心指标概览（注册用户、活跃预约、待审核申请）
- **用户管理** — 列表筛选、角色/状态编辑、锁定/暂停
- **预约管理** — 查看所有预约、按状态筛选、取消预约
- **修改申请** — 审批用户提交的信息修改（批准/拒绝）
- **图书馆管理** — CRUD 图书馆信息
- **系统规则** — 配置开闭馆时间、预约上限、爽约阈值、暂停天数等 10 项参数
- **审计日志** — 管理员操作全记录，按类型和时间筛选

## 技术栈

| 层 | 技术 |
|---|------|
| **后端框架** | Spring Boot 4.0.6, Spring Security, Spring Data JPA |
| **数据库** | PostgreSQL 17 |
| **认证** | JWT 双 Token（access 2h + refresh 7d），jjwt 0.12.6 |
| **邮件** | Spring Mail + Mailpit（开发） |
| **前端框架** | Vue 3.5 (Composition API), TypeScript 6.0 |
| **构建工具** | Vite 8 |
| **UI 框架** | Tailwind CSS 4, Headless UI, Heroicons |
| **状态管理** | Pinia 3 |
| **HTTP 客户端** | Axios |
| **日期处理** | dayjs, @vuepic/vue-datepicker |
| **端到端测试** | Python 3.14, pytest 9, httpx |
| **前端测试** | Vitest, Playwright |
| **数据库迁移** | 手动 SQL（`server/sql/init/`） |
| **容器化** | Docker Compose（开发环境） |

## 快速开始

### 前置要求

- JDK 25+
- Node.js 22.12+
- Python 3.14+（用于 E2E 测试）
- Docker & Docker Compose（用于运行 PostgreSQL 和 Mailpit）
- Maven 3.9+

### 1. 启动基础设施

```bash
# 从项目根目录启动 PostgreSQL 和 Mailpit
docker compose up -d
```

这将启动：
- **PostgreSQL 17** — `localhost:5432`，自动执行 `server/sql/init/` 中的建表和数据初始化脚本
- **Mailpit** — SMTP `localhost:1025`，Web UI `http://localhost:8025`

### 2. 配置环境变量

项目根目录的 `.env` 已包含默认值：

```bash
DB_NAME=libseat
DB_USER=libseat
DB_PASSWORD=libseat123
JWT_SECRET=bXktc2VjcmV0LWtleS1mb3ItbGlic2VhdC1hcHAtMzJieXRlcyE=
APP_BASE_URL=http://localhost:8080
```

### 3. 启动后端

```bash
cd server
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`，API 前缀为 `/api/v1`。

### 4. 启动前端

```bash
cd web
npm install
npm run dev
```

前端开发服务器启动在 `http://localhost:5173`，API 请求自动代理到后端。

### 5. 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | A000001 | password |
| 学生 | 2024001 | password123 |

## 测试

### 后端单元测试 & 集成测试

```bash
cd server
mvn test                              # 全部测试
mvn test -Dtest=SeatRepositoryTest    # 单个测试类
```

集成测试使用 Testcontainers 启动真实 PostgreSQL 17 容器，每个测试方法在事务中运行并自动回滚。

### 前端测试

```bash
cd web
npm run test               # Vitest 单元测试
npx playwright test        # Playwright E2E 测试
```

### API 端到端测试

```bash
cd test
uv run pytest              # 运行全部 API 测试
uv run pytest tests/test_auth.py   # 单个模块
```

需要后端已在 `http://localhost:8080` 运行。

## API 概览

所有接口统一在 `/api/v1` 下，使用 RESTful 风格，统一响应格式：

```json
{
  "code": "00000",
  "message": "ok",
  "data": { ... }
}
```

`code` 为 `"00000"` 表示成功，其他为业务错误码。

### 主要接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/auth/login` | 登录 | — |
| POST | `/auth/refresh` | 刷新 Token | Refresh Token |
| GET | `/seats` | 查询座位 | — |
| POST | `/reservations` | 创建预约 | 用户 |
| POST | `/reservations/{id}/checkin` | 签到 | 用户 |
| POST | `/reservations/{id}/checkout` | 签退 | 用户 |
| DELETE | `/reservations/{id}` | 取消预约 | 用户 |
| GET | `/admin/users` | 用户列表 | 管理员 |
| PATCH | `/admin/users/{id}` | 编辑用户 | 管理员 |
| GET | `/admin/reservations` | 预约列表 | 管理员 |
| GET | `/admin/change-requests` | 修改申请列表 | 管理员 |
| POST | `/admin/change-requests/{id}/review` | 审批申请 | 管理员 |
| GET/POST/PUT/DELETE | `/admin/libraries` | 图书馆 CRUD | 管理员 |
| GET/PUT | `/admin/system-rules` | 系统规则 | 管理员 |
| GET | `/admin/audit-logs` | 审计日志 | 管理员 |

完整 API 文档见 [`server/docs/api-spec.md`](server/docs/api-spec.md)。

## 数据库

PostgreSQL 17，Schema 和种子数据由 `server/sql/init/` 目录管理，Docker Compose 启动时自动执行：

- `01_schema.sql` — 建表（14 张表 + 10 个 ENUM 类型 + 触发器）
- `02_seed.sql` — 种子数据（测试用户、图书馆、座位、系统规则）

## Docker 部署

```bash
# 生产模式（包含应用和前端容器）
docker compose --profile prod up -d

# 仅基础设施
docker compose up -d
```

## 项目文档

- [API 规范](server/docs/api-spec.md)
- [详细设计文档](server/docs/detailed-design.md)
- [概要设计文档](server/docs/概要设计文档.md)
- [产品需求规格说明书](docs/产品需求规格说明书.pdf)
- [用户需求说明书](docs/用户需求说明书.pdf)
- [后端测试方案](test/docs/test-plan.md)

## 错误码体系

5 位分层错误码（参考阿里巴巴 Java 开发手册）：

| 前缀 | 类别 | 示例 |
|------|------|------|
| A0xxx | 用户端错误 | A0600 密码错误, A0601 账号已锁定 |
| B0xxx | 业务规则错误 | B0100 该时段已被预约 |
| C0xxx | 服务端错误 | C0001 系统繁忙 |

## 许可证

Internal Use
