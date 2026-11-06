package com.ape.marathon.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具类（Apache POI XSSF，生成 .xlsx）
 * 直接写入 HttpServletResponse 输出流，不落盘
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 将秒数格式化为 mm:ss（分钟可超过 59，例如 12540 秒 → 209:00）
     *
     * @param seconds 净成绩秒数，可为 null
     * @return 格式化字符串；null 时返回空串
     */
    public static String formatSecondsToMmSs(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "";
        }
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    /**
     * 兼容 Long / Number / String 的秒数字段
     */
    public static String formatSecondsToMmSs(Object secondsObj) {
        if (secondsObj == null) {
            return "";
        }
        if (secondsObj instanceof Number) {
            return formatSecondsToMmSs(((Number) secondsObj).intValue());
        }
        try {
            return formatSecondsToMmSs(Integer.parseInt(String.valueOf(secondsObj)));
        } catch (NumberFormatException e) {
            return String.valueOf(secondsObj);
        }
    }

    /**
     * 写出 Excel 到响应流
     *
     * @param response  HTTP 响应
     * @param fileName  下载文件名（含中文，无需事先编码；方法内 URLEncoder）
     * @param sheetName 工作表名称
     * @param headers   表头列名
     * @param rows      数据行（每行长度应与 headers 一致）
     */
    public static void export(HttpServletResponse response,
                              String fileName,
                              String sheetName,
                              String[] headers,
                              List<String[]> rows) throws IOException {
        // ---------- 响应头：告诉浏览器这是 Excel 附件下载 ----------
        // Content-Type：xlsx 标准 MIME
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        // 中文文件名：RFC 5987 的 filename*=UTF-8''xxx，并附带 filename= 兼容旧浏览器
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);

        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(sheetName == null ? "Sheet1" : sheetName);

            // 表头样式：加粗、居中
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 数据单元格样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 写表头
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写数据行
            if (rows != null) {
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    String[] cols = rows.get(r);
                    for (int c = 0; c < headers.length; c++) {
                        Cell cell = row.createCell(c);
                        String val = (cols != null && c < cols.length && cols[c] != null) ? cols[c] : "";
                        cell.setCellValue(val);
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            // 列宽自适应（中文略加宽，避免被裁切）
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                // 在自适应基础上再放宽约 20%，且设上下限
                int adjusted = (int) (width * 1.2);
                if (adjusted < 3000) {
                    adjusted = 3000;
                }
                if (adjusted > 15000) {
                    adjusted = 15000;
                }
                sheet.setColumnWidth(i, adjusted);
            }

            OutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
        } finally {
            workbook.close();
        }
    }
}
