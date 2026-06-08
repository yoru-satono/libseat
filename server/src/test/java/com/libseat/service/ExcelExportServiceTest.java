package com.libseat.service;

import com.libseat.dto.admin.AdminReservationResponse;
import com.libseat.dto.reservation.ReservationResponse;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportServiceTest {

    private final ExcelExportService service = new ExcelExportService();

    private ReservationResponse sampleResp() {
        return new ReservationResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                "A001", "总馆", (short) 2, SeatArea.QUIET,
                LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(11, 0),
                ReservationStatus.COMPLETED,
                OffsetDateTime.now(), null, null, OffsetDateTime.now());
    }

    private AdminReservationResponse sampleAdminResp() {
        return new AdminReservationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(), "S001", "张三",
                UUID.randomUUID(), "A001", "总馆", (short) 2,
                LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(11, 0),
                SeatArea.QUIET, ReservationStatus.COMPLETED,
                OffsetDateTime.now(), null, OffsetDateTime.now());
    }

    // ── 用户端导出 ────────────────────────────────────────────────────────

    @Test
    void exportReservations_emptyList_returnsValidWorkbook() throws IOException {
        byte[] data = service.exportReservations(List.of());
        assertThat(data).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1); // 仅表头行
        }
    }

    @Test
    void exportReservations_headerRowCorrect() throws IOException {
        byte[] data = service.exportReservations(List.of());
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("序号");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("图书馆");
            assertThat(header.getCell(9).getStringCellValue()).isEqualTo("状态");
        }
    }

    @Test
    void exportReservations_oneRow_dataCorrect() throws IOException {
        byte[] data = service.exportReservations(List.of(sampleResp()));
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2); // 表头 + 1 条
            Row row = sheet.getRow(1);
            assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(1); // 序号
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("总馆");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("A001");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("安静区");
            assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(120); // 时长
            assertThat(row.getCell(9).getStringCellValue()).isEqualTo("已完成");
        }
    }

    // ── 管理员端导出 ───────────────────────────────────────────────────────

    @Test
    void exportAdminReservations_headerRowCorrect() throws IOException {
        byte[] data = service.exportAdminReservations(List.of());
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("序号");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("学号/工号");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("姓名");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("图书馆");
        }
    }

    @Test
    void exportAdminReservations_oneRow_userFieldsCorrect() throws IOException {
        byte[] data = service.exportAdminReservations(List.of(sampleAdminResp()));
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("S001");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("张三");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("总馆");
        }
    }
}
