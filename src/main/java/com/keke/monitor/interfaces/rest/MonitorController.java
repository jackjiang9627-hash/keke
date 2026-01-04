package com.keke.monitor.interfaces.rest;

import com.keke.monitor.application.dto.CreateTaskRequest;
import com.keke.monitor.application.dto.MonitorSnapshotDTO;
import com.keke.monitor.application.dto.MonitorTaskDTO;
import com.keke.monitor.application.service.MonitorApplicationService;
import com.keke.monitor.application.service.MonitorExcelService;
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
import java.util.ArrayList;
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
        log.info("执行单次系统监控检测。");
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
     * 批量启动任务
     * @param taskIds 任务ID列表
     * @return 操作后的任务列表
     */
    @PostMapping("/tasks/start")
    public List<MonitorTaskDTO> startTasks(@RequestBody List<Long> taskIds) {
        log.info("批量启动监控任务: {}", taskIds);
        List<MonitorTaskDTO> results = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                results.add(monitorApplicationService.startTask(taskId));
            } catch (Exception e) {
                log.error("启动任务失败: {}", taskId, e);
            }
        }
        return results;
    }
    
    /**
     * 批量暂停任务
     * @param taskIds 任务ID列表
     * @return 操作后的任务列表
     */
    @PostMapping("/tasks/pause")
    public List<MonitorTaskDTO> pauseTasks(@RequestBody List<Long> taskIds) {
        log.info("批量暂停监控任务: {}", taskIds);
        List<MonitorTaskDTO> results = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                results.add(monitorApplicationService.pauseTask(taskId));
            } catch (Exception e) {
                log.error("暂停任务失败: {}", taskId, e);
            }
        }
        return results;
    }
    
    /**
     * 批量恢复任务
     * @param taskIds 任务ID列表
     * @return 操作后的任务列表
     */
    @PostMapping("/tasks/resume")
    public List<MonitorTaskDTO> resumeTasks(@RequestBody List<Long> taskIds) {
        log.info("批量恢复监控任务: {}", taskIds);
        List<MonitorTaskDTO> results = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                results.add(monitorApplicationService.resumeTask(taskId));
            } catch (Exception e) {
                log.error("恢复任务失败: {}", taskId, e);
            }
        }
        return results;
    }
    
    /**
     * 批量停止任务
     * @param taskIds 任务ID列表
     * @return 操作后的任务列表
     */
    @PostMapping("/tasks/stop")
    public List<MonitorTaskDTO> stopTasks(@RequestBody List<Long> taskIds) {
        log.info("批量停止监控任务: {}", taskIds);
        List<MonitorTaskDTO> results = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                results.add(monitorApplicationService.stopTask(taskId));
            } catch (Exception e) {
                log.error("停止任务失败: {}", taskId, e);
            }
        }
        return results;
    }
    
    /**
     * 批量删除任务
     * @param taskIds 任务ID列表
     * @return 成功响应
     */
    @PostMapping("/tasks/delete")
    public ResponseEntity<Void> deleteTasks(@RequestBody List<Long> taskIds) {
        log.info("批量删除监控任务: {}", taskIds);
        for (Long taskId : taskIds) {
            try {
                monitorApplicationService.deleteTask(taskId);
            } catch (Exception e) {
                log.error("删除任务失败: {}", taskId, e);
            }
        }
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
    
    /**
     * 批量导出任务报告
     * @param taskIds 任务ID列表
     * @return Excel文件，每个Sheet对应一个任务，包含该任务的所有监控数据
     */
    @PostMapping("/export/tasks")
    public ResponseEntity<byte[]> exportTasksReport(@RequestBody List<Long> taskIds) throws IOException {
        log.info("批量导出任务报告: {}", taskIds);
        
        List<MonitorTaskDTO> tasks = new ArrayList<>();
        // 每个任务对应的快照历史列表
        List<List<MonitorSnapshotDTO>> taskSnapshotsList = new ArrayList<>();
        
        for (Long taskId : taskIds) {
            try {
                MonitorTaskDTO task = monitorApplicationService.getTask(taskId);
                tasks.add(task);
                
                // 获取该任务的所有监控快照历史
                List<MonitorSnapshotDTO> taskSnapshots = monitorApplicationService.getTaskSnapshotHistory(taskId);
                taskSnapshotsList.add(taskSnapshots);
            } catch (Exception e) {
                log.warn("跳过无效的任务ID: {}", taskId, e);
            }
        }
        
        if (tasks.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        byte[] excelBytes = monitorExcelService.exportTaskReports(tasks, taskSnapshotsList);
        
        // 根据任务数量生成文件名
        String filename;
        if (tasks.size() == 1) {
            filename = "任务报告_" + tasks.get(0).getName() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        } else {
            filename = "任务报告_" + tasks.size() + "个_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
