package com.suios.admin.common.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelUtils {

    private ExcelUtils() {
    }

    public static void export(String fileName,
                              List<String> headers,
                              List<? extends List<?>> rows,
                              HttpServletResponse response)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Sheet1");
            writeHeaders(sheet.createRow(0), headers);
            writeRows(sheet, rows);
            autosize(sheet, headers.size());

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xlsx"
            );
            workbook.write(response.getOutputStream());
        }
    }

    private static void writeHeaders(Row row, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
        }
    }

    private static void writeRows(XSSFSheet sheet, List<? extends List<?>> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<?> values = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                Cell cell = row.createCell(columnIndex);
                Object value = values.get(columnIndex);
                cell.setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
    }

    private static void autosize(XSSFSheet sheet, int headerCount) {
        for (int i = 0; i < headerCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
