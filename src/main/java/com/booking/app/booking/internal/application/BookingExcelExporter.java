package com.booking.app.booking.internal.application;

import com.booking.app.booking.internal.domain.Booking;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class BookingExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private static final String[] HEADERS = {
        "Booking ID",
        "Customer Name",
        "Customer Email",
        "Starts At (UTC)",
        "Ends At (UTC)",
        "Status",
        "Total Amount",
        "Currency"
    };

    private BookingExcelExporter() {}

    static byte[] exportToExcel(List<Booking> bookings) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Bookings Report");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle totalAmountStyle = createTotalAmountStyle(workbook);

            int rowNum = 0;

            // Title Row
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Service Bookings Report");
            titleCell.setCellStyle(titleStyle);

            rowNum++; // Empty spacing row

            // Header Row
            Row headerRow = sheet.createRow(rowNum++);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (Booking booking : bookings) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, booking.getPublicId().toString(), textStyle);
                createCell(row, 1, booking.getCustomerName(), textStyle);
                createCell(row, 2, booking.getCustomerEmail(), textStyle);
                createCell(row, 3, DATE_FORMATTER.format(booking.getStartsAt()), textStyle);
                createCell(row, 4, DATE_FORMATTER.format(booking.getEndsAt()), textStyle);
                createCell(row, 5, booking.getStatus().name(), textStyle);

                Cell amountCell = row.createCell(6);
                amountCell.setCellValue(booking.getTotalAmount().doubleValue());
                amountCell.setCellStyle(currencyStyle);

                createCell(row, 7, booking.getCurrency(), textStyle);

                grandTotal = grandTotal.add(booking.getTotalAmount());
            }

            // Summary Row
            Row totalRow = sheet.createRow(rowNum++);
            Cell totalLabelCell = totalRow.createCell(5);
            totalLabelCell.setCellValue("Total Revenue:");
            totalLabelCell.setCellStyle(totalLabelStyle);

            Cell totalAmountCell = totalRow.createCell(6);
            totalAmountCell.setCellValue(grandTotal.doubleValue());
            totalAmountCell.setCellStyle(totalAmountStyle);

            createCell(
                    totalRow, 7, bookings.isEmpty() ? "" : bookings.getFirst().getCurrency(), totalLabelStyle);

            // Auto-size all columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel report", e);
        }
    }

    private static void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        applyBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createTotalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private static CellStyle createTotalAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        return style;
    }

    private static void applyBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }
}
