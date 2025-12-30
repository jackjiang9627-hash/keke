package com.loganalyzer.assistant.application.service;

import com.loganalyzer.assistant.application.dto.CaseInputDTO;
import com.loganalyzer.assistant.application.dto.CaseOutputDTO;
import com.loganalyzer.shared.application.dto.PageDTO;
import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.service.CaseDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 案例Excel导入导出服务
 */
@Slf4j
@Service
public class CaseExcelService {

    private static final String[] HEADERS = {"标题", "摘要", "超链接", "详细内容"};
    
    private final CaseApplicationService caseApplicationService;
    private final CaseDomainService caseDomainService;
    
    public CaseExcelService(CaseApplicationService caseApplicationService,
                           CaseDomainService caseDomainService) {
        this.caseApplicationService = caseApplicationService;
        this.caseDomainService = caseDomainService;
    }
    
    /**
     * 导出案例到Excel
     * 每个模块一个sheet
     */
    public byte[] exportToExcel() throws IOException {
        List<String> modules = caseApplicationService.getAllModuleNames();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle contentStyle = createContentStyle(workbook);
            
            for (String moduleName : modules) {
                if (moduleName == null || moduleName.isEmpty()) {
                    moduleName = "默认";
                }
                
                // 获取该模块的所有案例
                PageDTO<CaseOutputDTO> pageResult = caseApplicationService.getCasesByModule(moduleName, 0, 1000);
                List<CaseOutputDTO> cases = pageResult.getContent();
                
                // 创建sheet（处理特殊字符）
                String sheetName = sanitizeSheetName(moduleName);
                Sheet sheet = workbook.createSheet(sheetName);
                
                // 设置列宽
                sheet.setColumnWidth(0, 8000);  // 标题
                sheet.setColumnWidth(1, 12000); // 摘要
                sheet.setColumnWidth(2, 8000);  // 超链接
                sheet.setColumnWidth(3, 15000); // 详细内容
                
                // 创建表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < HEADERS.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(HEADERS[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // 填充数据
                int rowNum = 1;
                for (CaseOutputDTO caseDto : cases) {
                    Row row = sheet.createRow(rowNum++);
                    
                    Cell titleCell = row.createCell(0);
                    titleCell.setCellValue(caseDto.getTitle());
                    titleCell.setCellStyle(contentStyle);
                    
                    Cell summaryCell = row.createCell(1);
                    summaryCell.setCellValue(caseDto.getSummary());
                    summaryCell.setCellStyle(contentStyle);
                    
                    Cell hyperlinkCell = row.createCell(2);
                    hyperlinkCell.setCellValue(caseDto.getHyperlink() != null ? caseDto.getHyperlink() : "");
                    hyperlinkCell.setCellStyle(contentStyle);
                    
                    Cell contentCell = row.createCell(3);
                    contentCell.setCellValue(caseDto.getContent() != null ? caseDto.getContent() : "");
                    contentCell.setCellStyle(contentStyle);
                }
                
                log.info("导出模块 [{}] 案例 {} 条", moduleName, cases.size());
            }
            
            // 如果没有模块，创建一个空的默认sheet
            if (modules.isEmpty()) {
                Sheet sheet = workbook.createSheet("默认");
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < HEADERS.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(HEADERS[i]);
                    cell.setCellStyle(headerStyle);
                }
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    /**
     * 从Excel导入案例
     * sheet名称作为模块名
     * 如果案例已存在（标题+模块相同），则更新；否则新增
     */
    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        int successCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            
            int sheetCount = workbook.getNumberOfSheets();
            log.info("开始导入Excel，共 {} 个sheet", sheetCount);
            
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String moduleName = sheet.getSheetName();
                
                if (moduleName == null || moduleName.isEmpty()) {
                    moduleName = "默认";
                }
                
                log.info("处理sheet: {}", moduleName);
                
                // 从第2行开始读取（跳过表头）
                for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row == null) continue;
                    
                    try {
                        String title = getCellStringValue(row.getCell(0));
                        String summary = getCellStringValue(row.getCell(1));
                        String hyperlink = getCellStringValue(row.getCell(2));
                        String content = getCellStringValue(row.getCell(3));
                        
                        // 标题为必填
                        if (title == null || title.trim().isEmpty()) {
                            continue; // 跳过空行
                        }
                        if (summary == null || summary.trim().isEmpty()) {
                            summary = title; // 摘要默认使用标题
                        }
                        
                        CaseInputDTO inputDTO = new CaseInputDTO();
                        inputDTO.setTitle(title.trim());
                        inputDTO.setSummary(summary.trim());
                        inputDTO.setHyperlink(hyperlink != null ? hyperlink.trim() : null);
                        inputDTO.setContent(content != null ? content.trim() : null);
                        inputDTO.setModuleName(moduleName);
                        
                        // 使用领域服务检查是否已存在
                        Optional<CaseEntry> existing = caseDomainService.findExisting(title.trim(), moduleName);
                        
                        if (existing.isPresent()) {
                            // 已存在，更新
                            caseApplicationService.updateCase(existing.get().getId().value(), inputDTO);
                            updateCount++;
                            log.debug("更新已存在案例: title={}, module={}", title, moduleName);
                        } else {
                            // 不存在，新增（绕过应用服务的去重检查）
                            caseApplicationService.addCaseWithoutDuplicateCheck(inputDTO);
                            successCount++;
                        }
                        
                    } catch (Exception e) {
                        skipCount++;
                        errors.add(String.format("Sheet[%s] 第%d行: %s", moduleName, rowNum + 1, e.getMessage()));
                        log.warn("导入行失败: sheet={}, row={}, error={}", moduleName, rowNum, e.getMessage());
                    }
                }
            }
        }
        
        log.info("Excel导入完成: 新增={}, 更新={}, 跳过={}", successCount, updateCount, skipCount);
        return new ImportResult(successCount, updateCount, skipCount, errors);
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }
    
    /**
     * 清理sheet名称（移除非法字符）
     */
    private String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) return "默认";
        // Excel sheet名称不能包含: \ / ? * [ ]
        String sanitized = name.replaceAll("[\\\\/?*\\[\\]]", "_");
        // 长度不能超过31
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    /**
     * 创建内容样式
     */
    private CellStyle createContentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }
    
    /**
     * 导入结果
     */
    public record ImportResult(int successCount, int updateCount, int skipCount, List<String> errors) {
        public boolean hasErrors() {
            return skipCount > 0;
        }
        
        public int totalProcessed() {
            return successCount + updateCount;
        }
    }
}
