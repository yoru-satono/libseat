package com.libseat.service;

import com.libseat.dto.admin.AdminReservationResponse;
import com.libseat.dto.reservation.ReservationResponse;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] USER_HEADERS = {
            "序号", "图书馆", "座位号", "楼层", "区域",
            "预约日期", "开始时间", "结束时间", "时长（分钟）", "状态",
            "签到时间", "取消时间", "取消原因", "创建时间"
    };

    private static final String[] ADMIN_HEADERS = {
            "序号", "学号/工号", "姓名", "图书馆", "座位号", "楼层", "区域",
            "预约日期", "开始时间", "结束时间", "时长（分钟）", "状态",
            "签到时间", "取消时间", "取消原因", "创建时间"
    };

    public byte[] exportReservations(List<ReservationResponse> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("预约记录");
            CellStyle headerStyle = buildHeaderStyle(wb);

            writeRow(sheet.createRow(0), USER_HEADERS, headerStyle);
            sheet.createFreezePane(0, 1);

            int rowIdx = 1;
            for (ReservationResponse r : rows) {
                Row row = sheet.createRow(rowIdx);
                int col = 0;
                cell(row, col++, rowIdx);
                cell(row, col++, r.libraryName());
                cell(row, col++, r.seatNo());
                cell(row, col++, r.floor());
                cell(row, col++, translateArea(r.area()));
                cell(row, col++, r.date() != null ? r.date().format(DATE_FMT) : "");
                cell(row, col++, r.startTime() != null ? r.startTime().format(TIME_FMT) : "");
                cell(row, col++, r.endTime() != null ? r.endTime().format(TIME_FMT) : "");
                cell(row, col++, durationMinutes(r.startTime(), r.endTime()));
                cell(row, col++, translateStatus(r.status()));
                cell(row, col++, formatDateTime(r.checkinAt()));
                cell(row, col++, formatDateTime(r.cancelledAt()));
                cell(row, col++, r.cancelReason() != null ? r.cancelReason() : "");
                cell(row, col,   formatDateTime(r.createdAt()));
                rowIdx++;
            }

            autoSize(sheet, USER_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Excel 生成失败", e);
        }
    }

    public byte[] exportAdminReservations(List<AdminReservationResponse> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("预约记录");
            CellStyle headerStyle = buildHeaderStyle(wb);

            writeRow(sheet.createRow(0), ADMIN_HEADERS, headerStyle);
            sheet.createFreezePane(0, 1);

            int rowIdx = 1;
            for (AdminReservationResponse r : rows) {
                Row row = sheet.createRow(rowIdx);
                int col = 0;
                cell(row, col++, rowIdx);
                cell(row, col++, r.userNo());
                cell(row, col++, r.realName());
                cell(row, col++, r.libraryName());
                cell(row, col++, r.seatNo());
                cell(row, col++, r.floor());
                cell(row, col++, translateArea(r.area()));
                cell(row, col++, r.date() != null ? r.date().format(DATE_FMT) : "");
                cell(row, col++, r.startTime() != null ? r.startTime().format(TIME_FMT) : "");
                cell(row, col++, r.endTime() != null ? r.endTime().format(TIME_FMT) : "");
                cell(row, col++, durationMinutes(r.startTime(), r.endTime()));
                cell(row, col++, translateStatus(r.status()));
                cell(row, col++, formatDateTime(r.checkinAt()));
                cell(row, col++, formatDateTime(r.cancelledAt()));
                cell(row, col++, "");
                cell(row, col,   formatDateTime(r.createdAt()));
                rowIdx++;
            }

            autoSize(sheet, ADMIN_HEADERS.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new RuntimeException("Excel 生成失败", e);
        }
    }

    private CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void writeRow(Row row, String[] headers, CellStyle style) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 15000));
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static void cell(Row row, int col, Object value) {
        Cell c = row.createCell(col);
        if (value instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(value != null ? value.toString() : "");
        }
    }

    private static long durationMinutes(LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;
        return Duration.between(start, end).toMinutes();
    }

    private static String formatDateTime(OffsetDateTime dt) {
        return dt != null ? dt.toLocalDateTime().format(DT_FMT) : "";
    }

    private static String translateArea(SeatArea area) {
        if (area == null) return "";
        return switch (area) {
            case QUIET      -> "安静区";
            case DISCUSSION -> "研讨区";
            case COMPUTER   -> "机房区";
        };
    }

    private static String translateStatus(ReservationStatus status) {
        if (status == null) return "";
        return switch (status) {
            case ACTIVE     -> "待开始";
            case CHECKED_IN -> "已签到";
            case IN_USE     -> "使用中";
            case COMPLETED  -> "已完成";
            case CANCELLED  -> "已取消";
            case NO_SHOW    -> "爽约";
        };
    }
}
