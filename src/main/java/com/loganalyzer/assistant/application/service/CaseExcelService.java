package com.loganalyzer.assistant.application.service;

import com.loganalyzer.assistant.application.dto.CaseInputDTO;
import com.loganalyzer.assistant.application.dto.CaseOutputDTO;
import com.loganalyzer.shared.application.dto.PageDTO;
import com.loganalyzer.assistant.domain.entity.CaseEntry;
import com.loganalyzer.assistant.domain.service.CaseDomainService;
import com.loganalyzer.shared.infrastructure.excel.ExcelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private static final int[] COLUMN_WIDTHS = {8000, 12000, 8000, 15000};
    
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
        
        ExcelBuilder builder = ExcelBuilder.create();
        
        if (modules.isEmpty()) {
            // 没有模块，创建默认sheet
            builder.sheet("默认")
                    .headers(HEADERS)
                    .columnWidths(COLUMN_WIDTHS);
        } else {
            for (String moduleName : modules) {
                String sheetName = (moduleName == null || moduleName.isEmpty()) ? "默认" : moduleName;
                
                // 获取该模块的所有案例
                PageDTO<CaseOutputDTO> pageResult = caseApplicationService.getCasesByModule(moduleName, 0, 10000);
                List<CaseOutputDTO> cases = pageResult.getContent();
                
                ExcelBuilder.SheetBuilder sheetBuilder = builder.sheet(sheetName)
                        .headers(HEADERS)
                        .columnWidths(COLUMN_WIDTHS)
                        .data(cases, c -> new Object[]{
                                c.getTitle(),
                                c.getSummary(),
                                c.getHyperlink() != null ? c.getHyperlink() : "",
                                c.getContent() != null ? c.getContent() : ""
                        });
                
                log.info("导出模块 [{}] 案例 {} 条", sheetName, cases.size());
            }
        }
        
        return builder.build();
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
