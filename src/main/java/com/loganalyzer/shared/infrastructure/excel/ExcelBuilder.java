package com.loganalyzer.shared.infrastructure.excel;

import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * Excel导出建造者工具类
 * 使用建造者模式构建Excel文件
 * 
 * 使用示例:
 * <pre>
 * byte[] excel = ExcelBuilder.create()
 *     .sheet("用户列表")
 *         .headers("姓名", "年龄", "邮箱")
 *         .columnWidths(5000, 3000, 8000)
 *         .data(users, user -> new Object[]{user.getName(), user.getAge(), user.getEmail()})
 *     .sheet("订单列表")
 *         .headers("订单号", "金额", "状态")
 *         .data(orders, order -> new Object[]{order.getId(), order.getAmount(), order.getStatus()})
 *     .build();
 * </pre>
 */
public class ExcelBuilder {
    
    private final Workbook workbook;
    private final List<SheetBuilder> sheets = new ArrayList<>();
    private CellStyle headerStyle;
    private CellStyle contentStyle;
    private CellStyle titleStyle;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private ExcelBuilder() {
        this.workbook = new XSSFWorkbook();
        initStyles();
    }
    
    /**
     * 创建Excel建造者
     */
    public static ExcelBuilder create() {
        return new ExcelBuilder();
    }
    
    /**
     * 初始化默认样式
     */
    private void initStyles() {
        // 表头样式
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 内容样式
        contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        
        // 标题样式
        titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }
    
    /**
     * 开始构建新的Sheet
     */
    public SheetBuilder sheet(String name) {
        SheetBuilder sheetBuilder = new SheetBuilder(this, sanitizeSheetName(name));
        sheets.add(sheetBuilder);
        return sheetBuilder;
    }
    
    /**
     * 构建Excel并返回字节数组
     */
    public byte[] build() throws IOException {
        // 构建所有sheet
        for (SheetBuilder sheetBuilder : sheets) {
            sheetBuilder.buildSheet();
        }
        
        // 如果没有sheet，创建一个空的
        if (sheets.isEmpty()) {
            workbook.createSheet("Sheet1");
        }
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            workbook.close();
            return out.toByteArray();
        }
    }
    
    /**
     * 清理sheet名称
     */
    private String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) return "Sheet";
        String sanitized = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }
    
    // Getters for styles
    CellStyle getHeaderStyle() { return headerStyle; }
    CellStyle getContentStyle() { return contentStyle; }
    CellStyle getTitleStyle() { return titleStyle; }
    Workbook getWorkbook() { return workbook; }
    
    /**
     * Sheet建造者
     */
    public static class SheetBuilder {
        private final ExcelBuilder parent;
        private final String sheetName;
        private String title;
        private final List<SheetContent> contents = new ArrayList<>();
        
        SheetBuilder(ExcelBuilder parent, String sheetName) {
            this.parent = parent;
            this.sheetName = sheetName;
        }
        
        /**
         * 设置Sheet标题（居中合并显示）
         */
        public SheetBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * 设置表头
         */
        public SheetBuilder headers(String... headers) {
            contents.add(new TableHeader(headers));
            return this;
        }
        
        /**
         * 设置列宽（单位：1/256字符宽度，如5000约等于18个字符）
         */
        public SheetBuilder columnWidths(int... widths) {
            contents.add(new ColumnWidths(widths));
            return this;
        }
        
        /**
         * 添加数据行
         */
        public SheetBuilder row(Object... values) {
            contents.add(new DataRow(values));
            return this;
        }
        
        /**
         * 批量添加数据
         */
        public <T> SheetBuilder data(List<T> items, Function<T, Object[]> mapper) {
            if (items != null) {
                for (T item : items) {
                    contents.add(new DataRow(mapper.apply(item)));
                }
            }
            return this;
        }
        
        /**
         * 添加键值对（用于信息展示）
         */
        public SheetBuilder keyValue(String key, Object value) {
            contents.add(new KeyValuePair(key, value));
            return this;
        }
        
        /**
         * 添加空行
         */
        public SheetBuilder emptyRow() {
            contents.add(new EmptyRow());
            return this;
        }
        
        /**
         * 添加分组标题
         */
        public SheetBuilder section(String sectionTitle) {
            contents.add(new SectionTitle(sectionTitle));
            return this;
        }
        
        /**
         * 开始新的Sheet
         */
        public SheetBuilder sheet(String name) {
            return parent.sheet(name);
        }
        
        /**
         * 完成构建
         */
        public byte[] build() throws IOException {
            return parent.build();
        }
        
        /**
         * 返回父级建造者
         */
        public ExcelBuilder end() {
            return parent;
        }
        
        /**
         * 构建Sheet内容
         */
        void buildSheet() {
            Sheet sheet = parent.getWorkbook().createSheet(sheetName);
            int rowNum = 0;
            int[] currentColumnWidths = null;
            
            // 处理标题
            if (title != null && !title.isEmpty()) {
                Row titleRow = sheet.createRow(rowNum++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(title);
                titleCell.setCellStyle(parent.getTitleStyle());
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
                rowNum++; // 空行
            }
            
            // 设置默认列宽
            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 12000);
            
            // 按顺序处理所有内容
            for (SheetContent content : contents) {
                if (content instanceof EmptyRow) {
                    rowNum++;
                } else if (content instanceof KeyValuePair kv) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(kv.key);
                    row.createCell(1).setCellValue(formatValue(kv.value));
                } else if (content instanceof SectionTitle section) {
                    Row row = sheet.createRow(rowNum++);
                    Cell cell = row.createCell(0);
                    cell.setCellValue(section.title);
                    cell.setCellStyle(parent.getHeaderStyle());
                } else if (content instanceof ColumnWidths cw) {
                    currentColumnWidths = cw.widths;
                    for (int i = 0; i < cw.widths.length; i++) {
                        sheet.setColumnWidth(i, cw.widths[i]);
                    }
                } else if (content instanceof TableHeader th) {
                    Row headerRow = sheet.createRow(rowNum++);
                    for (int i = 0; i < th.headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(th.headers[i]);
                        cell.setCellStyle(parent.getHeaderStyle());
                    }
                } else if (content instanceof DataRow dr) {
                    if (dr.values.length == 0) {
                        rowNum++;
                        continue;
                    }
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < dr.values.length; i++) {
                        Cell cell = row.createCell(i);
                        setCellValue(cell, dr.values[i]);
                        cell.setCellStyle(parent.getContentStyle());
                    }
                }
            }
        }
        
        private void setCellValue(Cell cell, Object value) {
            if (value == null) {
                cell.setCellValue("");
            } else if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else if (value instanceof LocalDateTime) {
                cell.setCellValue(((LocalDateTime) value).format(DATE_FORMAT));
            } else if (value instanceof Date) {
                cell.setCellValue((Date) value);
            } else {
                cell.setCellValue(String.valueOf(value));
            }
        }
        
        private String formatValue(Object value) {
            if (value == null) return "";
            if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).format(DATE_FORMAT);
            }
            return String.valueOf(value);
        }
        
        // 内容类型接口
        private interface SheetContent {}
        
        private record EmptyRow() implements SheetContent {}
        private record KeyValuePair(String key, Object value) implements SheetContent {}
        private record SectionTitle(String title) implements SheetContent {}
        private record TableHeader(String[] headers) implements SheetContent {}
        private record ColumnWidths(int[] widths) implements SheetContent {}
        private record DataRow(Object[] values) implements SheetContent {}
    }
}
