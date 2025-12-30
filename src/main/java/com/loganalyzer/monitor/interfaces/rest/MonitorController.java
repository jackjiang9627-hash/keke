package com.loganalyzer.monitor.interfaces.rest;

import com.loganalyzer.monitor.application.dto.CreateTaskRequest;
import com.loganalyzer.monitor.application.dto.MonitorSnapshotDTO;
import com.loganalyzer.monitor.application.dto.MonitorTaskDTO;
import com.loganalyzer.monitor.application.service.MonitorApplicationService;
import com.loganalyzer.monitor.application.service.MonitorExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 系统监控REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {
    
    private final MonitorApplicationService monitorApplicationService;
    private final MonitorExcelService monitorExcelService;
    
    /**
     * 获取当前系统快照
     */
    @GetMapping("/snapshot")
    public MonitorSnapshotDTO getCurrentSnapshot() {
        log.info("获取当前系统监控快照");
        return monitorApplicationService.getCurrentSnapshot();
    }
    
    /**
     * 执行单次监控检测
     */
    @PostMapping("/detect")
    public MonitorSnapshotDTO executeOnceDetect() {
        log.info("执行单次系统监控检测");
        return monitorApplicationService.executeOnceMonitor();
    }
    
    /**
     * 获取快照历史
     */
    @GetMapping("/history")
    public List<MonitorSnapshotDTO> getHistory(@RequestParam(defaultValue = "20") int limit) {
        return monitorApplicationService.getSnapshotHistory(limit);
    }
    
    // ==================== 任务管理 ====================
    
    /**
     * 创建监控任务
     */
    @PostMapping("/tasks")
    public MonitorTaskDTO createTask(@RequestBody @Valid CreateTaskRequest request) {
        log.info("创建监控任务: {}", request.getName());
        return monitorApplicationService.createTask(request);
    }
    
    /**
     * 获取所有任务
     */
    @GetMapping("/tasks")
    public List<MonitorTaskDTO> getAllTasks() {
        return monitorApplicationService.getAllTasks();
    }
    
    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{taskId}")
    public MonitorTaskDTO getTask(@PathVariable Long taskId) {
        return monitorApplicationService.getTask(taskId);
    }
    
    /**
     * 启动任务
     */
    @PostMapping("/tasks/{taskId}/start")
    public MonitorTaskDTO startTask(@PathVariable Long taskId) {
        log.info("启动监控任务: {}", taskId);
        return monitorApplicationService.startTask(taskId);
    }
    
    /**
     * 暂停任务
     */
    @PostMapping("/tasks/{taskId}/pause")
    public MonitorTaskDTO pauseTask(@PathVariable Long taskId) {
        log.info("暂停监控任务: {}", taskId);
        return monitorApplicationService.pauseTask(taskId);
    }
    
    /**
     * 恢复任务
     */
    @PostMapping("/tasks/{taskId}/resume")
    public MonitorTaskDTO resumeTask(@PathVariable Long taskId) {
        log.info("恢复监控任务: {}", taskId);
        return monitorApplicationService.resumeTask(taskId);
    }
    
    /**
     * 停止任务
     */
    @PostMapping("/tasks/{taskId}/stop")
    public MonitorTaskDTO stopTask(@PathVariable Long taskId) {
        log.info("停止监控任务: {}", taskId);
        return monitorApplicationService.stopTask(taskId);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        log.info("删除监控任务: {}", taskId);
        monitorApplicationService.deleteTask(taskId);
        return ResponseEntity.ok().build();
    }
    
    // ==================== 报告导出 ====================
    
    /**
     * 导出当前监控报告
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCurrentReport() throws IOException {
        log.info("导出当前监控报告");
        MonitorSnapshotDTO snapshot = monitorApplicationService.getCurrentSnapshot();
        byte[] excelBytes = monitorExcelService.exportReport(snapshot);
        
        String filename = "系统监控报告_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
    
    /**
     * 导出历史监控报告
     */
    @GetMapping("/export/history")
    public ResponseEntity<byte[]> exportHistoryReport(@RequestParam(defaultValue = "50") int limit) throws IOException {
        log.info("导出历史监控报告，条数: {}", limit);
        List<MonitorSnapshotDTO> snapshots = monitorApplicationService.getSnapshotHistory(limit);
        byte[] excelBytes = monitorExcelService.exportHistoryReport(snapshots);
        
        String filename = "监控历史报告_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
