package com.loganalyzer.log.interfaces.rest;

import com.loganalyzer.log.application.dto.LogInputDTO;
import com.loganalyzer.log.application.dto.LogOutputDTO;
import com.loganalyzer.log.application.dto.LogStatisticsDTO;
import com.loganalyzer.log.application.service.LogApplicationService;
import com.loganalyzer.shared.application.dto.PageDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志API控制器 - 接口层
 * 
 * DDD概念：接口层（Interface Layer）/ 用户接口层
 * - 处理HTTP请求和响应
 * - 调用应用服务
 * - 不包含业务逻辑
 * - 负责请求验证和响应格式化
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    private final LogApplicationService logApplicationService;

    public LogController(LogApplicationService logApplicationService) {
        this.logApplicationService = logApplicationService;
    }

    /**
     * 接收并处理单条日志
     * 
     * POST /api/v1/logs
     */
    @PostMapping
    public ResponseEntity<LogOutputDTO> receiveLog(@Valid @RequestBody LogInputDTO input) {
        log.info("REST API: 接收日志请求");
        LogOutputDTO result = logApplicationService.receiveAndProcess(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 批量接收并处理日志
     * 
     * POST /api/v1/logs/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<List<LogOutputDTO>> receiveBatchLogs(@Valid @RequestBody List<LogInputDTO> inputs) {
        List<LogOutputDTO> results = logApplicationService.batchReceiveAndProcess(inputs);
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * 根据ID查询日志
     * 
     * GET /api/v1/logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogOutputDTO> getLogById(@PathVariable String id) {
        LogOutputDTO result = logApplicationService.findById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 查询所有日志（分页）
     * 
     * GET /api/v1/logs?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<PageDTO<LogOutputDTO>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        
        List<LogOutputDTO> results;
        long total;
        
        if (keyword != null && !keyword.isEmpty()) {
            // 关键词搜索
            results = logApplicationService.searchByKeyword(keyword);
            total = results.size();
        } else if (level != null && !level.isEmpty()) {
            // 按级别查询
            results = logApplicationService.findByLevel(level);
            total = results.size();
        } else {
            // 分页查询
            results = logApplicationService.findAll(page, size);
            total = logApplicationService.count();
        }
        
        PageDTO<LogOutputDTO> pageResult = PageDTO.of(results, page, size, total);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * 根据日志级别查询
     * 
     * GET /api/v1/logs/level/{level}
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<LogOutputDTO>> getLogsByLevel(@PathVariable String level) {
        List<LogOutputDTO> results = logApplicationService.findByLevel(level);
        return ResponseEntity.ok(results);
    }

    /**
     * 根据应用名称查询
     * 
     * GET /api/v1/logs/application/{application}
     */
    @GetMapping("/application/{application}")
    public ResponseEntity<List<LogOutputDTO>> getLogsByApplication(@PathVariable String application) {
        List<LogOutputDTO> results = logApplicationService.findByApplication(application);
        return ResponseEntity.ok(results);
    }

    /**
     * 根据时间范围查询
     * 
     * GET /api/v1/logs/time-range?start=2024-01-01T00:00:00&end=2024-01-02T00:00:00
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<LogOutputDTO>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<LogOutputDTO> results = logApplicationService.findByTimeRange(start, end);
        return ResponseEntity.ok(results);
    }

    /**
     * 查询错误日志
     * 
     * GET /api/v1/logs/errors
     */
    @GetMapping("/errors")
    public ResponseEntity<List<LogOutputDTO>> getErrorLogs() {
        List<LogOutputDTO> results = logApplicationService.findErrors();
        return ResponseEntity.ok(results);
    }

    /**
     * 关键词搜索
     * 
     * GET /api/v1/logs/search?keyword=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<List<LogOutputDTO>> searchLogs(@RequestParam String keyword) {
        List<LogOutputDTO> results = logApplicationService.searchByKeyword(keyword);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取统计信息
     * 
     * GET /api/v1/logs/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<LogStatisticsDTO> getStatistics() {
        LogStatisticsDTO statistics = logApplicationService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * 删除日志
     * 
     * DELETE /api/v1/logs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable String id) {
        logApplicationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 重新处理未清洗的日志
     * 
     * POST /api/v1/logs/reprocess
     */
    @PostMapping("/reprocess")
    public ResponseEntity<List<LogOutputDTO>> reprocessUncleaned() {
        List<LogOutputDTO> results = logApplicationService.reprocessUncleaned();
        return ResponseEntity.ok(results);
    }

    /**
     * 上传日志文件并清洗
     * 
     * POST /api/v1/logs/upload
     * 支持的文件类型: .log, .txt, .json
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadLogFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "application", required = false) String application,
            @RequestParam(value = "environment", required = false, defaultValue = "prod") String environment) {
        
        log.info("REST API: 接收日志文件上传请求, filename={}, size={}", 
                file.getOriginalFilename(), file.getSize());
        
        // 验证文件
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件不能为空"
            ));
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.matches(".*\\.(log|txt|json|csv)$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "不支持的文件类型，请上传 .log, .txt, .json 或 .csv 文件"
            ));
        }
        
        // 限制文件大小 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "文件大小不能超过10MB"
            ));
        }
        
        try {
            List<LogOutputDTO> results = logApplicationService.uploadAndProcessFile(file, application, environment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", String.format("成功处理 %d 条日志", results.size()),
                    "count", results.size(),
                    "filename", filename != null ? filename : "unknown",
                    "logs", results
            ));
        } catch (Exception e) {
            log.error("处理上传文件失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "处理文件失败: " + e.getMessage()
            ));
        }
    }
}
