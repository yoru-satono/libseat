-- ================================================================
-- 图书馆座位预约系统 数据库结构
-- PostgreSQL 17
-- ================================================================

-- ----------------------------------------------------------------
-- 工具函数：自动更新 updated_at
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------
-- ENUM 类型
-- ----------------------------------------------------------------
CREATE TYPE user_role          AS ENUM ('STUDENT', 'TEACHER', 'ADMIN');
CREATE TYPE user_status        AS ENUM ('INACTIVE', 'ACTIVE', 'LOCKED', 'SUSPENDED');
CREATE TYPE seat_area          AS ENUM ('QUIET', 'DISCUSSION', 'COMPUTER');
CREATE TYPE seat_status_t      AS ENUM ('AVAILABLE', 'UNAVAILABLE');
CREATE TYPE reservation_status AS ENUM ('ACTIVE', 'CHECKED_IN', 'IN_USE', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
CREATE TYPE waitlist_status    AS ENUM ('WAITING', 'NOTIFIED', 'EXPIRED', 'CONVERTED');
CREATE TYPE token_type         AS ENUM ('ACTIVATION', 'PASSWORD_RESET', 'EMAIL_CHANGE');
CREATE TYPE change_req_status  AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE notification_type  AS ENUM (
    'RESERVATION_SUCCESS', 'RESERVATION_CANCELLED', 'CHECKIN_REMINDER',
    'NO_SHOW_WARNING', 'ACCOUNT_LOCKED', 'ACCOUNT_SUSPENDED',
    'WAITLIST_AVAILABLE', 'RENEWAL_SUCCESS', 'SYSTEM'
);

-- ----------------------------------------------------------------
-- 1. 图书馆
-- ----------------------------------------------------------------
CREATE TABLE libraries (
    library_id  UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    logo_url    VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (library_id)
);

-- ----------------------------------------------------------------
-- 2. 用户（含管理员）
-- ----------------------------------------------------------------
CREATE TABLE users (
    user_id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_no            VARCHAR(20)  NOT NULL UNIQUE,   -- 学号/工号
    real_name          VARCHAR(50)  NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    email              VARCHAR(100) NOT NULL UNIQUE,
    pending_email      VARCHAR(100),                  -- 待验证的新邮箱
    phone              VARCHAR(20),
    department         VARCHAR(100),                  -- 所属院系/部门
    role               user_role    NOT NULL DEFAULT 'STUDENT',
    status             user_status  NOT NULL DEFAULT 'INACTIVE',
    failed_login_count SMALLINT     NOT NULL DEFAULT 0,
    locked_until       TIMESTAMPTZ,                   -- 锁定解除时间（5次错误锁30分钟）
    suspended_until    TIMESTAMPTZ,                   -- 暂停预约截止时间（爽约超阈值）
    no_show_count      SMALLINT     NOT NULL DEFAULT 0,
    email_verified_at  TIMESTAMPTZ,
    last_login_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------
-- 3. 邮箱验证 & 密码重置令牌
-- ----------------------------------------------------------------
CREATE TABLE email_tokens (
    token_id   UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token      CHAR(64)    NOT NULL UNIQUE,
    type       token_type  NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_id),
    CONSTRAINT fk_et_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_email_tokens_user ON email_tokens (user_id);

-- ----------------------------------------------------------------
-- 4. 座位
-- ----------------------------------------------------------------
CREATE TABLE seats (
    seat_id      UUID         NOT NULL DEFAULT gen_random_uuid(),
    library_id   UUID         NOT NULL,
    seat_no      VARCHAR(20)  NOT NULL,               -- 座位编号，如 3F-A-012
    floor        SMALLINT     NOT NULL,
    area         seat_area    NOT NULL,
    has_computer BOOLEAN      NOT NULL DEFAULT FALSE,
    has_power    BOOLEAN      NOT NULL DEFAULT FALSE,
    has_window   BOOLEAN      NOT NULL DEFAULT FALSE,
    status       seat_status_t NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE=正常, UNAVAILABLE=停用
    pos_x        NUMERIC(7,2),                        -- 平面图坐标（前端可视化用）
    pos_y        NUMERIC(7,2),
    qr_token     CHAR(64)     UNIQUE,                 -- 座位二维码唯一标识
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seat_id),
    UNIQUE (library_id, seat_no),
    CONSTRAINT fk_seat_library FOREIGN KEY (library_id) REFERENCES libraries(library_id)
);

CREATE INDEX idx_seats_floor_area ON seats (library_id, floor, area, status);

CREATE TRIGGER trg_seats_updated_at
    BEFORE UPDATE ON seats
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------
-- 5. 预约
-- ----------------------------------------------------------------
CREATE TABLE reservations (
    reservation_id UUID               NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID               NOT NULL,
    seat_id        UUID               NOT NULL,
    date           DATE               NOT NULL,
    start_time     TIME               NOT NULL,
    end_time       TIME               NOT NULL,
    status         reservation_status NOT NULL DEFAULT 'ACTIVE',
    parent_id      UUID,                              -- 续约时关联的父预约
    checkin_at     TIMESTAMPTZ,
    cancelled_at   TIMESTAMPTZ,
    cancel_reason  VARCHAR(255),
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reservation_id),
    CONSTRAINT fk_res_user   FOREIGN KEY (user_id)   REFERENCES users(user_id),
    CONSTRAINT fk_res_seat   FOREIGN KEY (seat_id)   REFERENCES seats(seat_id),
    CONSTRAINT fk_res_parent FOREIGN KEY (parent_id) REFERENCES reservations(reservation_id)
);

CREATE INDEX idx_res_user_date ON reservations (user_id, date, start_time, end_time);
CREATE INDEX idx_res_seat_date ON reservations (seat_id, date, start_time, end_time);
CREATE INDEX idx_res_status    ON reservations (status, date);

CREATE TRIGGER trg_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------
-- 6. 等待队列
-- ----------------------------------------------------------------
CREATE TABLE waitlists (
    waitlist_id UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    seat_id     UUID            NOT NULL,
    date        DATE            NOT NULL,
    start_time  TIME            NOT NULL,
    end_time    TIME            NOT NULL,
    status      waitlist_status NOT NULL DEFAULT 'WAITING',
    notified_at TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (waitlist_id),
    CONSTRAINT fk_wl_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_wl_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id)
);

CREATE INDEX idx_wl_seat_date ON waitlists (seat_id, date, start_time, status, created_at);

-- ----------------------------------------------------------------
-- 7. 系统规则配置（library_id IS NULL = 全局默认）
-- ----------------------------------------------------------------
CREATE TABLE system_rules (
    rule_id               BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    library_id            UUID        UNIQUE,          -- NULL=全局规则，非NULL=该馆覆盖规则
    open_time_start       TIME        NOT NULL DEFAULT '07:00:00',
    open_time_end         TIME        NOT NULL DEFAULT '22:00:00',
    advance_days_max      SMALLINT    NOT NULL DEFAULT 7,   -- 最大提前预约天数
    single_min_minutes    SMALLINT    NOT NULL DEFAULT 30,  -- 单次最短时长（分钟）
    single_max_hours      SMALLINT    NOT NULL DEFAULT 4,   -- 单次最长时长（小时）
    daily_max_hours       SMALLINT    NOT NULL DEFAULT 8,   -- 每日累计最长时长（小时）
    checkin_early_minutes SMALLINT    NOT NULL DEFAULT 10,  -- 可提前签到分钟数
    checkin_late_minutes  SMALLINT    NOT NULL DEFAULT 15,  -- 签到宽限分钟数（超时爽约）
    no_show_threshold     SMALLINT    NOT NULL DEFAULT 3,   -- 触发暂停的爽约次数
    suspend_days          SMALLINT    NOT NULL DEFAULT 7,   -- 暂停预约天数
    updated_by            UUID,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (rule_id),
    CONSTRAINT fk_rule_library    FOREIGN KEY (library_id) REFERENCES libraries(library_id),
    CONSTRAINT fk_rule_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

CREATE TRIGGER trg_system_rules_updated_at
    BEFORE UPDATE ON system_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ----------------------------------------------------------------
-- 8. 用户关键信息修改申请（学号/姓名/院系需管理员审核）
-- ----------------------------------------------------------------
CREATE TABLE user_change_requests (
    request_id  UUID              NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID              NOT NULL,
    field_name  VARCHAR(50)       NOT NULL,  -- user_no / real_name / department
    old_value   VARCHAR(255),
    new_value   VARCHAR(255)      NOT NULL,
    status      change_req_status NOT NULL DEFAULT 'PENDING',
    handled_by  UUID,
    handle_note VARCHAR(255),
    created_at  TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at  TIMESTAMPTZ,
    PRIMARY KEY (request_id),
    CONSTRAINT fk_ucr_user       FOREIGN KEY (user_id)    REFERENCES users(user_id),
    CONSTRAINT fk_ucr_handled_by FOREIGN KEY (handled_by) REFERENCES users(user_id)
);

CREATE INDEX idx_ucr_user_status ON user_change_requests (user_id, status);

-- ----------------------------------------------------------------
-- 9. 站内通知
-- ----------------------------------------------------------------
CREATE TABLE notifications (
    notification_id UUID              NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID              NOT NULL,
    type            notification_type NOT NULL,
    title           VARCHAR(100)      NOT NULL,
    content         TEXT              NOT NULL,
    related_id      UUID,             -- 关联对象 ID（如 reservation_id）
    is_read         BOOLEAN           NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user_unread ON notifications (user_id, is_read, created_at);

-- ----------------------------------------------------------------
-- 10. 管理员操作审计日志（只追加，不修改）
-- ----------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id      BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    admin_id    UUID        NOT NULL,
    action_type VARCHAR(50) NOT NULL,  -- 如 UNLOCK_USER / RESET_PASSWORD / UPDATE_RULE
    target_type VARCHAR(50) NOT NULL,  -- user / seat / reservation / rule
    target_id   TEXT        NOT NULL,
    detail      JSONB,                 -- 变更前后字段快照
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_log_admin FOREIGN KEY (admin_id) REFERENCES users(user_id)
);

CREATE INDEX idx_audit_admin      ON audit_logs (admin_id);
CREATE INDEX idx_audit_target     ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

-- ----------------------------------------------------------------
-- 初始化全局规则
-- ----------------------------------------------------------------
INSERT INTO system_rules (library_id, open_time_start, open_time_end,
    advance_days_max, single_min_minutes, single_max_hours, daily_max_hours,
    checkin_early_minutes, checkin_late_minutes, no_show_threshold, suspend_days)
VALUES (NULL, '07:00:00', '22:00:00', 7, 30, 4, 8, 10, 15, 3, 7);
