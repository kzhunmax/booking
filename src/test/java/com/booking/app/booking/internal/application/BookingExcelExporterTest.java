package com.booking.app.booking.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.booking.app.booking.internal.domain.Booking;
import com.booking.app.booking.internal.domain.BookingInterval;
import com.booking.app.booking.internal.domain.BookingPricing;
import com.booking.app.booking.internal.domain.CustomerDetails;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingExcelExporterTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    @DisplayName("Should generate valid Excel workbook with headers, data, and totals")
    void shouldGenerateValidExcelWorkbook() throws IOException {
        Booking booking = new Booking(
                UUID.randomUUID(),
                new CustomerDetails("john@example.com", "John Doe"),
                new BookingInterval(NOW.plusSeconds(3600), NOW.plusSeconds(7200)),
                NOW,
                new BookingPricing(BigDecimal.valueOf(150.0), "USD"));

        byte[] bytes = BookingExcelExporter.exportToExcel(List.of(booking));

        assertThat(bytes).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Bookings Report");
            assertThat(sheet).isNotNull();

            // Title row
            Row titleRow = sheet.getRow(0);
            assertThat(titleRow.getCell(0).getStringCellValue()).isEqualTo("Service Bookings Report");

            // Header row
            Row headerRow = sheet.getRow(2);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Booking ID");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Customer Name");
            assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("Total Amount");

            // Data row
            Row dataRow = sheet.getRow(3);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("John Doe");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("john@example.com");
            assertThat(dataRow.getCell(6).getNumericCellValue()).isEqualTo(150.0);

            // Summary row
            Row totalRow = sheet.getRow(4);
            assertThat(totalRow.getCell(5).getStringCellValue()).isEqualTo("Total Revenue:");
            assertThat(totalRow.getCell(6).getNumericCellValue()).isEqualTo(150.0);
        }
    }
}
