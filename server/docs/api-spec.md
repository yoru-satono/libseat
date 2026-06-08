# 图书馆座位预约系统 — RESTful API 规范

## 目录

1. [基本约定](#1-基本约定)
2. [认证机制](#2-认证机制)
3. [统一响应格式](#3-统一响应格式)
4. [错误码定义](#4-错误码定义)
5. [分页规范](#5-分页规范)
6. [接口列表](#6-接口列表)
7. [枚举值参考](#7-枚举值参考)

---

## 1. 基本约定

### 1.1 Base URL

```
/api/v1
```

### 1.2 URL 命名

| 规则 | 示例 |
|------|------|
| 资源用**复数名词** | `/seats`、`/reservations` |
| 单词用**连字符**分隔 | `/change-requests` |
| 子资源用路径嵌套 | `/reservations/{id}/checkin` |
| 禁止动词出现在路径中 | ~~`/getSeats`~~、~~`/createReservation`~~ |

### 1.3 HTTP 方法语义

| 方法 | 语义 | 幂等 |
|------|------|------|
| GET | 查询资源 | 是 |
| POST | 创建资源 / 触发动作 | 否 |
| PUT | 全量替换资源 | 是 |
| PATCH | 部分更新资源 | 否 |
| DELETE | 删除资源 | 是 |

### 1.4 日期与时间

所有日期时间字段统一使用 **ISO 8601** 格式，携带时区：

```
日期时间：2026-05-08T14:30:00+08:00
仅日期：  2026-05-08
仅时间：  14:30
```

### 1.5 字段命名

请求体与响应体字段统一使用 **camelCase**。

---

## 2. 认证机制

采用 **JWT Bearer Token**，双 Token 方案。

### 2.1 请求头

```
Authorization: Bearer <accessToken>
```

### 2.2 Token 说明

| Token | 有效期 | 说明 |
|-------|--------|------|
| accessToken | 2 小时 | 携带在每次请求头中 |
| refreshToken | 7 天 | 仅用于刷新 accessToken，存于客户端 |

### 2.3 Token 刷新流程

1. 请求返回 `A0201 Token 已过期` 时，客户端自动调用刷新接口
2. 刷新成功后重试原请求
3. 刷新失败（refreshToken 过期或无效，返回 `A0202`）则跳转登录页

### 2.4 权限角色

| 角色 | 可访问范围 |
|------|-----------|
| 匿名用户 | 图书馆浏览、座位查询（只读） |
| 注册用户（`STUDENT` / `TEACHER`） | 所有用户功能 |
| 管理员（`ADMIN`） | 所有功能 + 后台管理 |

---

## 3. 统一响应格式

**所有接口**均返回如下结构，**HTTP 状态码统一为 `200`**，业务成功与否通过 `code` 字段区分。

### 3.1 通用结构

```json
{
  "code": "00000",
  "message": "success",
  "data": {},
  "timestamp": "2026-05-08T14:30:00+08:00"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 业务状态码，`"00000"` 表示成功 |
| `message` | string | 提示信息，错误时为可读错误描述 |
| `data` | any | 响应数据，无数据时为 `null` |
| `timestamp` | string | 服务器响应时间 |

### 3.2 成功示例

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "id": "e5f3a1b2-...",
    "seatNo": "3F-A-012",
    "date": "2026-05-08",
    "startTime": "09:00",
    "endTime": "11:00",
    "status": "ACTIVE"
  },
  "timestamp": "2026-05-08T08:45:00+08:00"
}
```

### 3.3 失败示例

```json
{
  "code": "B0210",
  "message": "超出每日累计预约时长上限",
  "data": null,
  "timestamp": "2026-05-08T08:45:00+08:00"
}
```

### 3.4 分页数据结构

分页查询的 `data` 字段统一使用以下结构：

```json
{
  "code": "00000",
  "message": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  },
  "timestamp": "2026-05-08T08:45:00+08:00"
}
```

### 3.5 文件下载响应

导出接口返回二进制文件流，不使用上述 JSON 包装格式：

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="reservations-2026-05-08.xlsx"
```

> 若导出过程发生业务错误（如超出条数限制），仍返回标准 JSON 格式的错误响应。

---

## 4. 错误码定义

错误码共 5 位，参考阿里巴巴《Java开发手册》分类：

```
一级（2位）：错误来源
  00 — 成功
  A0 — 用户端错误
  B0 — 业务规则错误
  C0 — 服务端错误

二级（2位）：错误类型
三级（1位）：具体错误
```

### 4.1 成功

| 错误码 | 说明 |
|--------|------|
| `00000` | 成功 |

### 4.2 用户端错误（A0）

| 错误码 | 说明 | 客户端处理建议 |
|--------|------|---------------|
| `A0100` | 未登录 / 未提供 Token | 跳转登录页 |
| `A0201` | Access Token 已过期 | 调 `/auth/refresh` 刷新后重试 |
| `A0202` | Refresh Token 已过期或无效 | 清除本地 Token，跳转登录页 |
| `A0301` | 无权限访问该资源 | 提示无权限 |
| `A0400` | 请求参数校验失败 | 显示具体字段错误 |
| `A0401` | 必填参数缺失 | — |
| `A0402` | 参数格式错误 | — |
| `A0410` | 资源不存在 | 提示「内容不存在」 |
| `A0420` | 导出数据量超过上限（5000条） | 提示缩小筛选范围 |
| `A0500` | 学号 / 工号已存在 | 提示重复 |
| `A0501` | 邮箱已被注册 | 提示重复 |
| `A0502` | 图书馆名称已存在 | 提示重复 |
| `A0600` | 账号或密码错误 | 显示错误提示 |
| `A0601` | 账号已锁定，请稍后重试 | 显示锁定提示 |
| `A0602` | 账号已暂停预约权限 | 显示暂停提示 |
| `A0603` | 账号未激活 | 提示前往邮箱激活 |

### 4.3 业务规则错误（B0）

| 错误码 | 说明 |
|--------|------|
| `B0100` | 座位不可用（已预约 / 维修中） |
| `B0200` | 预约时段冲突 |
| `B0201` | 超出最大提前预约天数 |
| `B0202` | 单次预约时长不合规 |
| `B0203` | 预约时间超出图书馆开放时间 |
| `B0210` | 超出每日累计预约时长上限 |
| `B0300` | 预约状态不可取消（非 ACTIVE） |
| `B0301` | 预约开始前 30 分钟内不可取消 |
| `B0400` | 不在签到时间窗口内 |
| `B0500` | 该时段已有人预约，无法续约 |
| `B0501` | 不在续约时间窗口内（预约结束前 15 分钟内方可续约） |
| `B0600` | 已在该时段等待队列中 |
| `B0601` | 等待记录状态不可取消 |

### 4.4 服务端错误（C0）

| 错误码 | 说明 |
|--------|------|
| `C0001` | 服务器内部错误 |
| `C0002` | 服务暂时不可用 |

---

## 5. 分页规范

### 5.1 请求参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | integer | `1` | 页码，从 1 开始 |
| `pageSize` | integer | `20` | 每页条数，最大 `100` |

### 5.2 示例

```
GET /api/v1/reservations?page=2&pageSize=10
```

---

## 6. 接口列表

### 6.1 认证 `/auth`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/auth/register` | 匿名 | 用户注册，发送激活邮件 |
| POST | `/auth/activate` | 匿名 | 邮箱激活账号 |
| POST | `/auth/login` | 匿名 | 用户登录，返回 Token |
| POST | `/auth/logout` | 用户 | 登出（客户端清除 Token） |
| POST | `/auth/refresh` | 匿名 | 使用 refreshToken 刷新 accessToken |
| POST | `/auth/password/reset-request` | 匿名 | 申请重置密码（发送邮件） |
| POST | `/auth/password/reset` | 匿名 | 使用邮件令牌重置密码 |
| POST | `/auth/email/confirm` | 匿名 | 使用邮件令牌确认邮箱变更 |

---

#### POST `/auth/register`

**请求体：**

```json
{
  "userNo":     "S202401001",
  "realName":   "王小明",
  "password":   "Pass1234",
  "email":      "user@example.com",
  "phone":      "13800000000",
  "department": "计算机学院"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `userNo` | string | 是 | 最长 20 字符 |
| `realName` | string | 是 | 最长 50 字符 |
| `password` | string | 是 | 8–100 字符，须同时包含字母和数字 |
| `email` | string | 是 | 合法邮箱格式，最长 100 字符 |
| `phone` | string | 否 | 最长 20 字符 |
| `department` | string | 否 | 最长 100 字符 |

**响应 data：** `null`（注册成功后发送激活邮件）

---

#### POST `/auth/activate` / POST `/auth/email/confirm`

**请求体：**

```json
{ "token": "<邮件中的激活令牌>" }
```

**响应 data：** `null`

---

#### POST `/auth/login`

**请求体：**

```json
{ "userNo": "S202401001", "password": "Pass1234" }
```

**响应 data（TokenPairResponse）：**

```json
{
  "accessToken":  "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn":    7200
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `accessToken` | string | Bearer Token，有效期 2 小时 |
| `refreshToken` | string | 刷新用，有效期 7 天 |
| `expiresIn` | integer | accessToken 有效秒数（7200） |

---

#### POST `/auth/refresh`

**请求体：**

```json
{ "refreshToken": "eyJhbGci..." }
```

**响应 data：** 同 TokenPairResponse（含新的 accessToken 和 refreshToken）

---

#### POST `/auth/password/reset-request`

**请求体：**

```json
{ "email": "user@example.com" }
```

**响应 data：** `null`

---

#### POST `/auth/password/reset`

**请求体：**

```json
{
  "token":       "<邮件中的重置令牌>",
  "newPassword": "NewPass1234"
}
```

| 字段 | 约束 |
|------|------|
| `token` | 必填 |
| `newPassword` | 必填，8–100 字符，须同时包含字母和数字 |

**响应 data：** `null`

---

### 6.2 用户 `/users/me`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/users/me` | 用户 | 获取当前用户信息 |
| PATCH | `/users/me` | 用户 | 修改密码 / 邮箱 / 手机号 |
| POST | `/users/me/change-requests` | 用户 | 申请修改学号 / 姓名 / 院系（需管理员审核） |
| GET | `/users/me/change-requests` | 用户 | 查询本人信息修改申请记录 |

---

#### GET `/users/me`

**响应 data（UserProfileResponse）：**

```json
{
  "id":          "b0000000-0000-0000-0000-000000000011",
  "userNo":      "S202401001",
  "realName":    "王小明",
  "email":       "user@example.com",
  "phone":       "13800000000",
  "department":  "计算机学院",
  "role":        "STUDENT",
  "status":      "ACTIVE",
  "noShowCount": 0,
  "lastLoginAt": "2026-05-08T10:00:00+08:00",
  "createdAt":   "2024-01-01T00:00:00+08:00"
}
```

---

#### PATCH `/users/me`

所有字段均可选，只传需要修改的字段。修改密码时 `oldPassword` 为必填。

**请求体：**

```json
{
  "email":       "new@example.com",
  "phone":       "13900000000",
  "oldPassword": "旧密码",
  "newPassword": "NewPass1234"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `email` | string | 否 | 合法邮箱格式，最长 100 字符；修改后触发邮件确认流程 |
| `phone` | string | 否 | 最长 20 字符 |
| `oldPassword` | string | 修改密码时必填 | — |
| `newPassword` | string | 修改密码时必填 | 8–100 字符，须同时包含字母和数字 |

**响应 data：** `null`

---

#### POST `/users/me/change-requests`

申请修改需要管理员审核的字段（学号、姓名、院系）。

**请求体：**

```json
{
  "fieldName": "realName",
  "newValue":  "王大明"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `fieldName` | string | 是 | 可选值：`userNo` / `realName` / `department`，最长 50 字符 |
| `newValue` | string | 是 | 最长 255 字符 |

**响应 data：** `null`

---

#### GET `/users/me/change-requests`

支持分页。

**响应 data（PageResult\<ChangeRequestResponse\>）：**

```json
{
  "id":          "ca000000-0000-0000-0000-000000000001",
  "fieldName":   "realName",
  "oldValue":    "王小明",
  "newValue":    "王大明",
  "status":      "PENDING",
  "handleNote":  null,
  "createdAt":   "2026-05-08T10:00:00+08:00",
  "handledAt":   null
}
```

---

### 6.3 图书馆 `/libraries`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/libraries` | 匿名 | 获取图书馆列表 |
| GET | `/libraries/{libraryId}` | 匿名 | 获取单个图书馆详情 |

---

#### GET `/libraries` / GET `/libraries/{libraryId}`

**响应 data（LibraryResponse）：**

```json
{
  "id":        "a0000000-0000-0000-0000-000000000001",
  "name":      "中心图书馆",
  "address":   "学院路1号",
  "logoUrl":   "https://example.com/logo.png",
  "createdAt": "2024-01-01T00:00:00+08:00"
}
```

`GET /libraries` 返回 `LibraryResponse[]`（不分页）。

---

### 6.4 座位 `/seats`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/seats` | 匿名 | 查询座位列表（支持筛选） |
| GET | `/seats/{seatId}` | 匿名 | 获取单个座位详情 |
| POST | `/seats` | 管理员 | 新增座位 |
| PUT | `/seats/{seatId}` | 管理员 | 修改座位信息 |
| DELETE | `/seats/{seatId}` | 管理员 | 删除座位 |

---

#### GET `/seats` 筛选参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `libraryId` | UUID | 按图书馆过滤 |
| `floor` | integer | 楼层 |
| `area` | string | 区域：`QUIET` / `DISCUSSION` / `COMPUTER` |
| `hasComputer` | boolean | 是否有电脑 |
| `hasPower` | boolean | 是否有电源 |
| `hasWindow` | boolean | 是否靠窗 |
| `date` | string | 查询日期，格式 `yyyy-MM-dd` |
| `startTime` | string | 开始时间，格式 `HH:mm` |
| `endTime` | string | 结束时间，格式 `HH:mm` |

> `date`、`startTime`、`endTime` 三个参数须同时提供，用于查询指定时间段内可用的座位。不传时返回全部座位（不过滤可用性）。

**响应 data（PageResult\<SeatResponse\>）：**

```json
{
  "id":          "c0000000-0000-0000-0000-000000000004",
  "libraryId":   "a0000000-0000-0000-0000-000000000001",
  "libraryName": "中心图书馆",
  "seatNo":      "2F-Q-001",
  "floor":       2,
  "area":        "QUIET",
  "hasComputer": false,
  "hasPower":    true,
  "hasWindow":   false,
  "status":      "AVAILABLE",
  "posX":        null,
  "posY":        null
}
```

---

#### POST `/seats`

**请求体：**

```json
{
  "libraryId":   "a0000000-0000-0000-0000-000000000001",
  "seatNo":      "2F-Q-010",
  "floor":       2,
  "area":        "QUIET",
  "hasComputer": false,
  "hasPower":    true,
  "hasWindow":   false,
  "posX":        null,
  "posY":        null
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `libraryId` | UUID | 是 | — |
| `seatNo` | string | 是 | 最长 20 字符 |
| `floor` | integer | 是 | — |
| `area` | string | 是 | `QUIET` / `DISCUSSION` / `COMPUTER` |
| `hasComputer` | boolean | 否 | 默认 `false` |
| `hasPower` | boolean | 否 | 默认 `false` |
| `hasWindow` | boolean | 否 | 默认 `false` |
| `posX` / `posY` | decimal | 否 | 平面图坐标，可选 |

**响应 data：** SeatResponse

---

#### PUT `/seats/{seatId}`

所有字段均可选，只传需要修改的字段。

**请求体（可选字段）：** `seatNo` / `floor` / `area` / `hasComputer` / `hasPower` / `hasWindow` / `status`（`AVAILABLE` / `UNAVAILABLE`）/ `posX` / `posY`

**响应 data：** SeatResponse

---

### 6.5 预约 `/reservations`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/reservations` | 用户 | 查询我的预约记录（分页） |
| GET | `/reservations/export` | 用户 | 导出我的预约记录（.xlsx） |
| GET | `/reservations/{reservationId}` | 用户 | 获取单条预约详情 |
| POST | `/reservations` | 用户 | 创建预约 |
| DELETE | `/reservations/{reservationId}` | 用户 | 取消预约 |
| POST | `/reservations/{reservationId}/checkin` | 用户 | 现场签到 |
| POST | `/reservations/{reservationId}/renew` | 用户 | 续约 |

---

#### GET `/reservations` 筛选参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `ACTIVE` / `CHECKED_IN` / `IN_USE` / `COMPLETED` / `CANCELLED` / `NO_SHOW` |
| `dateFrom` | string | 开始日期，格式 `yyyy-MM-dd` |
| `dateTo` | string | 结束日期，格式 `yyyy-MM-dd` |

**响应 data（PageResult\<ReservationResponse\>）：**

```json
{
  "id":           "d0000000-0000-0000-0000-000000000001",
  "seatId":       "c0000000-0000-0000-0000-000000000004",
  "seatNo":       "2F-Q-001",
  "libraryName":  "中心图书馆",
  "floor":        2,
  "area":         "QUIET",
  "date":         "2026-05-08",
  "startTime":    "09:00",
  "endTime":      "11:00",
  "status":       "ACTIVE",
  "checkinAt":    null,
  "cancelledAt":  null,
  "cancelReason": null,
  "createdAt":    "2026-05-07T20:00:00+08:00"
}
```

---

#### GET `/reservations/export`

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | 同上 |
| `dateFrom` | string | 开始日期 |
| `dateTo` | string | 结束日期 |

> 导出条数上限 5 000 条，超出返回 `A0420`。

---

#### POST `/reservations`

**请求体：**

```json
{
  "seatId":    "c0000000-0000-0000-0000-000000000004",
  "date":      "2026-05-09",
  "startTime": "09:00",
  "endTime":   "11:00"
}
```

**响应 data：** ReservationResponse

---

#### DELETE `/reservations/{reservationId}`

**请求体（可选）：**

```json
{ "cancelReason": "临时有事" }
```

**响应 data：** `null`

---

#### POST `/reservations/{reservationId}/checkin`

**请求体：**

```json
{ "qrToken": "<座位二维码中的令牌>" }
```

**响应 data：** `null`

---

#### POST `/reservations/{reservationId}/renew`

**请求体：**

```json
{ "newEndTime": "13:00" }
```

**响应 data：** ReservationResponse（新生成的续约预约记录）

---

### 6.6 等待队列 `/waitlists`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/waitlists` | 用户 | 加入等待队列 |
| GET | `/waitlists` | 用户 | 查询我的等待队列（分页） |
| DELETE | `/waitlists/{waitlistId}` | 用户 | 取消等待 |

---

#### POST `/waitlists`

**请求体：**

```json
{
  "seatId":    "c0000000-0000-0000-0000-000000000004",
  "date":      "2026-05-09",
  "startTime": "09:00",
  "endTime":   "11:00"
}
```

**响应 data：** WaitlistResponse

---

#### GET `/waitlists` 筛选参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | `WAITING` / `NOTIFIED` / `EXPIRED` / `CONVERTED` |

**响应 data（PageResult\<WaitlistResponse\>）：**

```json
{
  "id":          "e0000000-0000-0000-0000-000000000001",
  "seatId":      "c0000000-0000-0000-0000-000000000004",
  "seatNo":      "2F-Q-001",
  "libraryName": "中心图书馆",
  "floor":       2,
  "area":        "QUIET",
  "date":        "2026-05-09",
  "startTime":   "09:00",
  "endTime":     "11:00",
  "status":      "WAITING",
  "notifiedAt":  null,
  "expiresAt":   "2026-05-09T08:30:00+08:00",
  "createdAt":   "2026-05-07T20:00:00+08:00"
}
```

---

### 6.7 通知 `/notifications`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/notifications` | 用户 | 查询我的通知列表（分页） |
| GET | `/notifications/unread-count` | 用户 | 获取未读通知数量 |
| PATCH | `/notifications/{id}/read` | 用户 | 标记单条通知为已读 |
| PATCH | `/notifications/read-all` | 用户 | 全部标记为已读 |
| DELETE | `/notifications/{id}` | 用户 | 删除通知 |

---

#### GET `/notifications` 筛选参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `isRead` | boolean | `false` = 仅未读；`true` = 仅已读；不传 = 全部 |

**响应 data（PageResult\<NotificationResponse\>）：**

```json
{
  "id":        "ab000000-0000-0000-0000-000000000002",
  "type":      "CHECKIN_REMINDER",
  "title":     "签到提醒",
  "content":   "您在中心图书馆 2F-Q-001 的预约将于 15 分钟后开始，请及时签到。",
  "relatedId": "d0000000-0000-0000-0000-000000000001",
  "isRead":    false,
  "readAt":    null,
  "createdAt": "2026-05-08T08:45:00+08:00"
}
```

> `relatedId`：关联的预约 ID，可用于跳转预约详情页；部分系统通知该字段为 `null`。

---

#### GET `/notifications/unread-count`

**响应 data：**

```json
{ "count": 5 }
```

---

### 6.8 后台管理 `/admin`

所有接口均需 `ADMIN` 角色。

#### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users` | 查询用户列表（分页） |
| GET | `/admin/users/{userId}` | 获取用户详情 |
| PATCH | `/admin/users/{userId}` | 修改用户（解锁 / 重置密码 / 调整角色） |

**GET `/admin/users` 筛选参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `role` | string | `STUDENT` / `TEACHER` / `ADMIN` |
| `status` | string | `INACTIVE` / `ACTIVE` / `LOCKED` / `SUSPENDED` |

**响应 data（AdminUserResponse）**，在 UserProfileResponse 基础上额外包含：

```json
{
  "failedLoginCount": 0,
  "lockedUntil":      null,
  "suspendedUntil":   null
}
```

**PATCH `/admin/users/{userId}` 请求体（字段均可选）：**

```json
{
  "role":             "ADMIN",
  "status":           "ACTIVE",
  "newPassword":      "NewPass1234",
  "resetNoShowCount": true
}
```

---

#### 预约管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/reservations` | 查询全平台预约（分页） |
| GET | `/admin/reservations/export` | 导出预约记录（.xlsx，上限 5000 条） |
| GET | `/admin/reservations/{reservationId}` | 获取单条预约详情 |
| DELETE | `/admin/reservations/{reservationId}` | 取消预约（仅 ACTIVE），写审计日志 |

**GET `/admin/reservations` 筛选参数：** `status` / `dateFrom` / `dateTo`（同 6.5）

**GET `/admin/reservations/export` 筛选参数：** `userId` / `status` / `dateFrom` / `dateTo`

**响应 data（AdminReservationResponse）**，在 ReservationResponse 基础上额外包含：

```json
{
  "userId":   "b0000000-0000-0000-0000-000000000011",
  "userNo":   "S202401001",
  "realName": "王小明"
}
```

---

#### 信息修改申请

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/change-requests` | 查询待审核的信息修改申请（分页，默认只返回 PENDING） |
| PATCH | `/admin/change-requests/{requestId}` | 审核申请，写审计日志 |

**PATCH 请求体：**

```json
{ "action": "APPROVED", "handleNote": "已核实" }
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `action` | 是 | `APPROVED` / `REJECTED` |
| `handleNote` | 否 | 审核备注，最长 255 字符 |

**响应 data（AdminChangeRequestResponse）**，在 ChangeRequestResponse 基础上额外包含：`userId` / `userNo` / `realName`

---

#### 图书馆管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/libraries` | 获取图书馆列表 |
| GET | `/admin/libraries/{libraryId}` | 获取图书馆详情 |
| POST | `/admin/libraries` | 创建图书馆 |
| PUT | `/admin/libraries/{libraryId}` | 更新图书馆信息 |
| DELETE | `/admin/libraries/{libraryId}` | 删除图书馆（有关联座位时拒绝，返回 A0400） |

**POST / PUT 请求体：**

```json
{
  "name":    "北区图书馆",
  "address": "北区学院路2号",
  "logoUrl": "https://example.com/logo.png"
}
```

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `name` | string | 是（POST）/ 否（PUT） | 最长 100 字符，不可与现有图书馆重名 |
| `address` | string | 否 | 最长 255 字符 |
| `logoUrl` | string | 否 | 最长 500 字符 |

**响应 data：** LibraryResponse

---

#### 座位管理（管理员视角）

> 注：`POST /seats`、`PUT /seats/{id}`、`DELETE /seats/{id}` 实际需要管理员权限（见 6.4），此处不再重复列出。

---

#### 系统规则

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/system-rules` | 获取全局系统规则配置 |
| PUT | `/admin/system-rules` | 更新系统规则配置，自动写入审计日志 |

**GET `/admin/system-rules` 响应 data（SystemRulesResponse）：**

```json
{
  "id":                  1,
  "libraryId":           null,
  "libraryName":         null,
  "openTimeStart":       "08:00",
  "openTimeEnd":         "21:00",
  "advanceDaysMax":      7,
  "singleMinMinutes":    30,
  "singleMaxHours":      4,
  "dailyMaxHours":       8,
  "checkinEarlyMinutes": 15,
  "checkinLateMinutes":  30,
  "noShowThreshold":     3,
  "suspendDays":         7,
  "updatedAt":           "2026-05-01T00:00:00+08:00"
}
```

> `libraryId = null` 表示全局规则。若某图书馆有独立规则，则该图书馆的座位预约以图书馆规则为准。

**PUT `/admin/system-rules` 请求体（所有字段必填）：**

```json
{
  "openTimeStart":       "08:00",
  "openTimeEnd":         "21:00",
  "advanceDaysMax":      7,
  "singleMinMinutes":    30,
  "singleMaxHours":      4,
  "dailyMaxHours":       8,
  "checkinEarlyMinutes": 15,
  "checkinLateMinutes":  30,
  "noShowThreshold":     3,
  "suspendDays":         7
}
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `openTimeStart` | string | HH:mm | 开馆时间 |
| `openTimeEnd` | string | HH:mm | 闭馆时间 |
| `advanceDaysMax` | integer | 1–30 | 最多提前几天预约 |
| `singleMinMinutes` | integer | ≥15 | 单次最短预约分钟数 |
| `singleMaxHours` | integer | 1–12 | 单次最长预约小时数 |
| `dailyMaxHours` | integer | 1–24 | 每日累计预约上限小时数 |
| `checkinEarlyMinutes` | integer | ≥0 | 预约开始前可签到的分钟数 |
| `checkinLateMinutes` | integer | ≥0 | 预约开始后仍可签到的分钟数（超出标记爽约） |
| `noShowThreshold` | integer | ≥1 | 累计爽约次数达到此值时暂停账号 |
| `suspendDays` | integer | ≥1 | 暂停天数 |

**响应 data：** SystemRulesResponse

---

#### 审计日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/audit-logs` | 查询审计日志（分页，只读） |

**筛选参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `adminId` | UUID | 按操作管理员过滤 |
| `targetType` | string | 操作目标类型，如 `RESERVATION`、`USER` |
| `targetId` | string | 操作目标 ID |
| `dateFrom` | string | 开始日期 |
| `dateTo` | string | 结束日期 |

**响应 data（PageResult\<AuditLogResponse\>）：**

```json
{
  "id":         1,
  "adminId":    "b0000000-0000-0000-0000-000000000001",
  "adminName":  "管理员",
  "actionType": "CANCEL_RESERVATION",
  "targetType": "RESERVATION",
  "targetId":   "d0000000-0000-0000-0000-000000000001",
  "detail":     { "reason": "违规" },
  "ipAddress":  "127.0.0.1",
  "createdAt":  "2026-05-08T10:00:00+08:00"
}
```

---

## 7. 枚举值参考

### UserRole

| 值 | 说明 |
|----|------|
| `STUDENT` | 学生 |
| `TEACHER` | 教职工 |
| `ADMIN` | 管理员 |

### UserStatus

| 值 | 说明 |
|----|------|
| `INACTIVE` | 未激活（邮箱未验证） |
| `ACTIVE` | 正常 |
| `LOCKED` | 已锁定（多次密码错误，30 分钟后自动解锁） |
| `SUSPENDED` | 已暂停（爽约次数超限） |

### ReservationStatus

| 值 | 说明 |
|----|------|
| `ACTIVE` | 已预约，待签到 |
| `CHECKED_IN` | 已签到（在座位前） |
| `IN_USE` | 使用中 |
| `COMPLETED` | 已完成 |
| `CANCELLED` | 已取消 |
| `NO_SHOW` | 爽约（超时未签到） |

### SeatArea

| 值 | 说明 |
|----|------|
| `QUIET` | 安静区 |
| `DISCUSSION` | 讨论区 |
| `COMPUTER` | 机房区 |

### SeatStatus

| 值 | 说明 |
|----|------|
| `AVAILABLE` | 可用 |
| `UNAVAILABLE` | 不可用（维修中等） |

### WaitlistStatus

| 值 | 说明 |
|----|------|
| `WAITING` | 等待中 |
| `NOTIFIED` | 已通知（座位空出，等待用户预约） |
| `EXPIRED` | 已过期（通知后未在规定时间内预约） |
| `CONVERTED` | 已转化（成功预约） |

### NotificationType

| 值 | 说明 |
|----|------|
| `RESERVATION_SUCCESS` | 预约成功通知 |
| `RESERVATION_CANCELLED` | 预约被取消通知 |
| `CHECKIN_REMINDER` | 签到提醒 |
| `NO_SHOW_WARNING` | 爽约警告 |
| `ACCOUNT_LOCKED` | 账号被锁定通知 |
| `ACCOUNT_SUSPENDED` | 账号被暂停通知 |
| `WAITLIST_AVAILABLE` | 等待队列座位可用通知 |
| `RENEWAL_SUCCESS` | 续约成功通知 |
| `SYSTEM` | 系统通知 |

### ChangeRequestStatus

| 值 | 说明 |
|----|------|
| `PENDING` | 待审核 |
| `APPROVED` | 已批准 |
| `REJECTED` | 已拒绝 |
