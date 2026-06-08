package com.libseat.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 成功 ──────────────────────────────────────────────────────────────
    SUCCESS("00000", "success"),

    // ── A0 用户端错误 ──────────────────────────────────────────────────────
    NOT_LOGGED_IN("A0100", "未登录"),
    TOKEN_EXPIRED("A0201", "Token 已过期"),
    TOKEN_INVALID("A0202", "Token 无效"),
    FORBIDDEN("A0301", "无权限访问该资源"),
    PARAM_INVALID("A0400", "请求参数校验失败"),
    PARAM_MISSING("A0401", "必填参数缺失"),
    PARAM_FORMAT_ERROR("A0402", "参数格式错误"),
    RESOURCE_NOT_FOUND("A0410", "资源不存在"),
    EXPORT_LIMIT_EXCEEDED("A0420", "导出数据量超过上限（5000条）"),
    USER_NO_DUPLICATE("A0500", "学号/工号已存在"),
    EMAIL_DUPLICATE("A0501", "邮箱已被注册"),
    LIBRARY_NAME_DUPLICATE("A0502", "图书馆名称已存在"),
    WRONG_CREDENTIALS("A0600", "账号或密码错误"),
    ACCOUNT_LOCKED("A0601", "账号已锁定，请稍后重试"),
    ACCOUNT_SUSPENDED("A0602", "账号已暂停预约权限"),
    ACCOUNT_INACTIVE("A0603", "账号未激活"),

    // ── B0 业务规则错误 ────────────────────────────────────────────────────
    SEAT_UNAVAILABLE("B0100", "座位不可用"),
    RESERVATION_CONFLICT("B0200", "预约时段冲突"),
    RESERVATION_TOO_EARLY("B0201", "超出最大提前预约天数"),
    RESERVATION_DURATION_INVALID("B0202", "单次预约时长不合规"),
    RESERVATION_OUT_OF_HOURS("B0203", "预约时间超出图书馆开放时间"),
    DAILY_LIMIT_EXCEEDED("B0210", "超出每日累计预约时长上限"),
    RESERVATION_ALREADY_STARTED("B0300", "预约已开始，不可取消"),
    RESERVATION_CANCEL_TOO_LATE("B0301", "预约开始前30分钟内不可取消"),
    CHECKIN_WINDOW_CLOSED("B0400", "不在签到时间窗口内"),
    RENEW_SEAT_OCCUPIED("B0500", "该时段已有人预约，无法续约"),
    RENEW_WINDOW_CLOSED("B0501", "不在续约时间窗口内"),
    WAITLIST_DUPLICATE("B0600", "已在该时段等待队列中"),
    WAITLIST_NOT_CANCELLABLE("B0601", "等待记录状态不可取消"),

    // ── C0 服务端错误 ──────────────────────────────────────────────────────
    INTERNAL_ERROR("C0001", "服务器内部错误"),
    SERVICE_UNAVAILABLE("C0002", "服务暂时不可用");

    private final String code;
    private final String message;
}
