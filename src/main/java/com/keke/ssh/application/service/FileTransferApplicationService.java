package com.keke.ssh.application.service;

import com.keke.ssh.application.dto.TransferProgressDTO;
import com.keke.ssh.domain.entity.Device;
import com.keke.ssh.domain.exception.DeviceNotFoundException;
import com.keke.ssh.domain.port.FileTransferPort;
import com.keke.ssh.domain.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件传输应用服务
 * 
 * DDD概念：应用服务（Application Service）
 * - 编排领域对象完成用例
 * - 不包含业务逻辑
 * - 作为Controller与领域层/基础设施层的中介
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTransferApplicationService {
    
    private final FileTransferPort fileTransferPort;
    private final DeviceRepository deviceRepository;
    
    /**
     * 上传文件到多个设备
     */
    public List<TransferProgressDTO> uploadToDevices(byte[] fileContent, String fileName, 
            String remotePath, List<Long> deviceIds) {
        log.info("上传文件到多个设备: fileName={}, deviceCount={}", fileName, deviceIds.size());
        
        List<TransferProgressDTO> progresses = new ArrayList<>();
        
        for (Long deviceId : deviceIds) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new DeviceNotFoundException(deviceId));
            
            try {
                String transferId = fileTransferPort.startUpload(device, fileContent, fileName, remotePath);
                FileTransferPort.TransferInfo info = fileTransferPort.getProgress(transferId);
                progresses.add(toDTO(info));
            } catch (Exception e) {
                log.error("启动上传任务失败: deviceId={}, error={}", deviceId, e.getMessage());
            }
        }
        
        return progresses;
    }
    
    /**
     * 从设备下载文件
     */
    public List<TransferProgressDTO> downloadFromDevices(String remotePath, String localPath, 
            List<Long> deviceIds) {
        log.info("从设备下载文件: remotePath={}, deviceCount={}", remotePath, deviceIds.size());
        
        List<TransferProgressDTO> progresses = new ArrayList<>();
        
        for (Long deviceId : deviceIds) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new DeviceNotFoundException(deviceId));
            
            // 为每个设备创建不同的本地路径
            String deviceLocalPath = deviceIds.size() > 1 
                    ? localPath.replace(".", "_" + device.getHost() + ".")
                    : localPath;
            
            String transferId = fileTransferPort.startDownload(device, remotePath, deviceLocalPath);
            FileTransferPort.TransferInfo info = fileTransferPort.getProgress(transferId);
            progresses.add(toDTO(info));
        }
        
        return progresses;
    }
    
    /**
     * 恢复下载（断点续传）
     */
    public TransferProgressDTO resumeDownload(Long deviceId, String remotePath, String localPath) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        
        String transferId = fileTransferPort.resumeDownload(device, remotePath, localPath);
        FileTransferPort.TransferInfo info = fileTransferPort.getProgress(transferId);
        return toDTO(info);
    }
    
    /**
     * 获取传输进度
     */
    public TransferProgressDTO getProgress(String transferId) {
        FileTransferPort.TransferInfo info = fileTransferPort.getProgress(transferId);
        return info != null ? toDTO(info) : null;
    }
    
    /**
     * 批量获取传输进度
     */
    public List<TransferProgressDTO> getBatchProgress(List<String> transferIds) {
        return transferIds.stream()
                .map(fileTransferPort::getProgress)
                .filter(info -> info != null)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有活跃的传输任务
     */
    public Collection<TransferProgressDTO> getAllActiveTransfers() {
        Map<String, FileTransferPort.TransferInfo> transfers = fileTransferPort.getAllActiveTransfers();
        return transfers.values().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 取消传输
     */
    public void cancelTransfer(String transferId) {
        fileTransferPort.cancelTransfer(transferId);
        log.info("传输任务已取消: transferId={}", transferId);
    }
    
    /**
     * 转换为DTO
     */
    private TransferProgressDTO toDTO(FileTransferPort.TransferInfo info) {
        return TransferProgressDTO.builder()
                .transferId(info.transferId())
                .deviceId(info.deviceId())
                .deviceName(info.deviceName())
                .deviceHost(info.deviceHost())
                .fileName(info.fileName())
                .totalBytes(info.totalBytes())
                .transferredBytes(info.transferredBytes())
                .percentage(info.percentage())
                .status(info.status())
                .errorMessage(info.errorMessage())
                .startTime(info.startTime())
                .build();
    }
}
