package com.loganalyzer.ssh.interfaces.rest;

import com.loganalyzer.ssh.domain.entity.Device;
import com.loganalyzer.ssh.domain.valueobject.TransferProgress;
import com.loganalyzer.ssh.infrastructure.adapter.SftpTransferManager;
import com.loganalyzer.ssh.domain.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件传输控制器 - 支持进度显示和断点续传
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/transfer")
@RequiredArgsConstructor
public class FileTransferController {
    
    private final SftpTransferManager transferManager;
    private final DeviceRepository deviceRepository;
    
    /**
     * 上传文件到多个设备
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("remotePath") String remotePath,
            @RequestParam("deviceIds") List<Long> deviceIds) {
        
        List<String> transferIds = new ArrayList<>();
        List<TransferProgress> progresses = new ArrayList<>();
        
        // 先读取文件内容到内存，避免InputStream在异步任务执行前被关闭
        byte[] fileContent;
        try {
            fileContent = file.getBytes();
        } catch (Exception e) {
            log.error("读取上传文件失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "读取文件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
        
        for (Long deviceId : deviceIds) {
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isPresent()) {
                try {
                    Device device = deviceOpt.get();
                    String transferId = transferManager.startUpload(
                            device, 
                            fileContent,
                            file.getOriginalFilename(), 
                            remotePath
                    );
                    transferIds.add(transferId);
                    progresses.add(transferManager.getProgress(transferId));
                } catch (Exception e) {
                    log.error("启动上传任务失败: deviceId={}, error={}", deviceId, e.getMessage());
                }
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("transferIds", transferIds);
        result.put("progresses", progresses);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 从设备下载文件
     */
    @PostMapping("/download")
    public ResponseEntity<Map<String, Object>> downloadFile(
            @RequestBody Map<String, Object> request) {
        
        String remotePath = (String) request.get("remotePath");
        String localPath = (String) request.get("localPath");
        @SuppressWarnings("unchecked")
        List<Integer> deviceIdInts = (List<Integer>) request.get("deviceIds");
        List<Long> deviceIds = deviceIdInts.stream()
                .map(Integer::longValue)
                .toList();
        
        List<String> transferIds = new ArrayList<>();
        List<TransferProgress> progresses = new ArrayList<>();
        
        for (Long deviceId : deviceIds) {
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                // 为每个设备创建不同的本地路径
                String deviceLocalPath = localPath.replace(".", "_" + device.getHost() + ".");
                
                String transferId = transferManager.startDownload(device, remotePath, deviceLocalPath);
                transferIds.add(transferId);
                progresses.add(transferManager.getProgress(transferId));
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("transferIds", transferIds);
        result.put("progresses", progresses);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取传输进度
     */
    @GetMapping("/progress/{transferId}")
    public ResponseEntity<TransferProgress> getProgress(@PathVariable String transferId) {
        TransferProgress progress = transferManager.getProgress(transferId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 批量获取传输进度
     */
    @PostMapping("/progress/batch")
    public ResponseEntity<List<TransferProgress>> getBatchProgress(@RequestBody List<String> transferIds) {
        List<TransferProgress> progresses = new ArrayList<>();
        for (String id : transferIds) {
            TransferProgress progress = transferManager.getProgress(id);
            if (progress != null) {
                progresses.add(progress);
            }
        }
        return ResponseEntity.ok(progresses);
    }
    
    /**
     * 获取所有活跃的传输任务
     */
    @GetMapping("/active")
    public ResponseEntity<Collection<TransferProgress>> getActiveTransfers() {
        return ResponseEntity.ok(transferManager.getAllActiveTransfers().values());
    }
    
    /**
     * 取消/暂停传输
     */
    @PostMapping("/cancel/{transferId}")
    public ResponseEntity<Map<String, String>> cancelTransfer(@PathVariable String transferId) {
        transferManager.cancelTransfer(transferId);
        Map<String, String> result = new HashMap<>();
        result.put("status", "cancelled");
        result.put("transferId", transferId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 恢复下载（断点续传）
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> resumeDownload(@RequestBody Map<String, Object> request) {
        Long deviceId = ((Number) request.get("deviceId")).longValue();
        String remotePath = (String) request.get("remotePath");
        String localPath = (String) request.get("localPath");
        
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        String transferId = transferManager.resumeDownload(deviceOpt.get(), remotePath, localPath);
        TransferProgress progress = transferManager.getProgress(transferId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transferId", transferId);
        result.put("progress", progress);
        
        return ResponseEntity.ok(result);
    }
}
