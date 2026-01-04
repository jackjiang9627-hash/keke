package com.keke.ssh.interfaces.rest;

import com.keke.ssh.application.dto.TransferProgressDTO;
import com.keke.ssh.application.service.FileTransferApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件传输控制器 - 支持进度显示和断点续传
 * 
 * DDD概念：接口层控制器
 * - 仅负责请求/响应转换
 * - 通过应用服务编排业务流程
 */
@Slf4j
@RestController
@RequestMapping("/api/ssh/transfer")
@RequiredArgsConstructor
public class FileTransferController {
    
    private final FileTransferApplicationService fileTransferService;
    
    /**
     * 上传文件到多个设备
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("remotePath") String remotePath,
            @RequestParam("deviceIds") List<Long> deviceIds) {
        
        // 先读取文件内容到内存
        byte[] fileContent;
        try {
            fileContent = file.getBytes();
        } catch (Exception e) {
            log.error("读取上传文件失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "读取文件失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
        
        List<TransferProgressDTO> progresses = fileTransferService.uploadToDevices(
                fileContent, file.getOriginalFilename(), remotePath, deviceIds);
        
        List<String> transferIds = progresses.stream()
                .map(TransferProgressDTO::getTransferId)
                .collect(Collectors.toList());
        
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
        
        List<TransferProgressDTO> progresses = fileTransferService.downloadFromDevices(
                remotePath, localPath, deviceIds);
        
        List<String> transferIds = progresses.stream()
                .map(TransferProgressDTO::getTransferId)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("transferIds", transferIds);
        result.put("progresses", progresses);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取传输进度
     */
    @GetMapping("/progress/{transferId}")
    public ResponseEntity<TransferProgressDTO> getProgress(@PathVariable String transferId) {
        TransferProgressDTO progress = fileTransferService.getProgress(transferId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(progress);
    }
    
    /**
     * 批量获取传输进度
     */
    @PostMapping("/progress/batch")
    public ResponseEntity<List<TransferProgressDTO>> getBatchProgress(@RequestBody List<String> transferIds) {
        List<TransferProgressDTO> progresses = fileTransferService.getBatchProgress(transferIds);
        return ResponseEntity.ok(progresses);
    }
    
    /**
     * 获取所有活跃的传输任务
     */
    @GetMapping("/active")
    public ResponseEntity<Collection<TransferProgressDTO>> getActiveTransfers() {
        return ResponseEntity.ok(fileTransferService.getAllActiveTransfers());
    }
    
    /**
     * 取消/暂停传输
     */
    @PostMapping("/cancel/{transferId}")
    public ResponseEntity<Map<String, String>> cancelTransfer(@PathVariable String transferId) {
        fileTransferService.cancelTransfer(transferId);
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
        
        TransferProgressDTO progress = fileTransferService.resumeDownload(deviceId, remotePath, localPath);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transferId", progress.getTransferId());
        result.put("progress", progress);
        
        return ResponseEntity.ok(result);
    }
}
