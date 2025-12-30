package com.loganalyzer.assistant.interfaces.rest;

import com.loganalyzer.assistant.application.dto.CaseInputDTO;
import com.loganalyzer.assistant.application.dto.CaseOutputDTO;
import com.loganalyzer.shared.application.dto.PageDTO;
import com.loganalyzer.assistant.application.service.CaseApplicationService;
import com.loganalyzer.assistant.application.service.CaseExcelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 案例REST控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
public class CaseController {
    
    private final CaseApplicationService caseApplicationService;
    private final CaseExcelService caseExcelService;
    
    public CaseController(CaseApplicationService caseApplicationService,
                         CaseExcelService caseExcelService) {
        this.caseApplicationService = caseApplicationService;
        this.caseExcelService = caseExcelService;
    }
    
    /**
     * 添加案例
     */
    @PostMapping
    public ResponseEntity<CaseOutputDTO> addCase(@RequestBody CaseInputDTO input) {
        log.info("添加案例请求: {}", input.getTitle());
        CaseOutputDTO result = caseApplicationService.addCase(input);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新案例
     */
    @PutMapping("/{id}")
    public ResponseEntity<CaseOutputDTO> updateCase(
            @PathVariable String id, 
            @RequestBody CaseInputDTO input) {
        log.info("更新案例请求: id={}", id);
        CaseOutputDTO result = caseApplicationService.updateCase(id, input);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除案例
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable String id) {
        log.info("删除案例请求: id={}", id);
        caseApplicationService.deleteCase(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取单个案例
     */
    @GetMapping("/{id}")
    public ResponseEntity<CaseOutputDTO> getCase(@PathVariable String id) {
        CaseOutputDTO result = caseApplicationService.getCase(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取指定模块的案例列表（分页）
     */
    @GetMapping("/module/{moduleCode}")
    public ResponseEntity<PageDTO<CaseOutputDTO>> getCasesByModule(
            @PathVariable String moduleCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("获取模块案例: module={}, page={}, size={}", moduleCode, page, size);
        PageDTO<CaseOutputDTO> results = caseApplicationService.getCasesByModule(moduleCode, page, size);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取所有案例（分页）
     */
    @GetMapping
    public ResponseEntity<PageDTO<CaseOutputDTO>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageDTO<CaseOutputDTO> results = caseApplicationService.getAllCases(page, size);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 搜索案例（精确匹配 + 语义匹配）
     */
    @GetMapping("/search")
    public ResponseEntity<List<CaseOutputDTO>> searchCases(@RequestParam String query) {
        log.info("搜索案例: query={}", query);
        List<CaseOutputDTO> results = caseApplicationService.searchCases(query);
        return ResponseEntity.ok(results);
    }
    
    /**
     * 获取所有模块名称
     */
    @GetMapping("/modules")
    public ResponseEntity<List<String>> getModuleNames() {
        List<String> modules = caseApplicationService.getAllModuleNames();
        return ResponseEntity.ok(modules);
    }
    
    /**
     * 导出案例到Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            log.info("导出案例到Excel");
            byte[] excelData = caseExcelService.exportToExcel();
            
            String fileName = "案例库_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (IOException e) {
            log.error("导出失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 从Excel导入案例
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            log.info("导入Excel: {}", file.getOriginalFilename());
            
            // 验证文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "请上传Excel文件(.xlsx或.xls)"
                ));
            }
            
            CaseExcelService.ImportResult result = caseExcelService.importFromExcel(file);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "successCount", result.successCount(),
                "updateCount", result.updateCount(),
                "skipCount", result.skipCount(),
                "errors", result.errors(),
                "message", String.format("导入完成：新增 %d 条，更新 %d 条，跳过 %d 条", 
                    result.successCount(), result.updateCount(), result.skipCount())
            ));
        } catch (IOException e) {
            log.error("导入失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "导入失败: " + e.getMessage()
            ));
        }
    }
}
