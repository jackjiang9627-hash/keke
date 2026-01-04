package com.keke.ssh.domain.port;

import com.keke.ssh.domain.entity.Device;

import java.util.Map;

/**
 * 文件传输端口接口
 * 
 * DDD概念：端口（Port）
 * - 定义领域层需要的文件传输能力
 * - 具体实现由基础设施层的适配器提供
 * - 保持领域层与技术实现的解耦
 */
public interface FileTransferPort {
    
    /**
     * 传输状态
     */
    enum TransferStatus {
        PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    }
    
    /**
     * 传输进度信息
     */
    record TransferInfo(
        String transferId,
        Long deviceId,
        String deviceName,
        String deviceHost,
        String fileName,
        long totalBytes,
        long transferredBytes,
        int percentage,
        String status,
        String errorMessage,
        long startTime
    ) {}
    
    /**
     * 开始上传文件
     * @param device 目标设备
     * @param fileContent 文件内容
     * @param fileName 文件名
     * @param remotePath 远程路径
     * @return 传输任务ID
     */
    String startUpload(Device device, byte[] fileContent, String fileName, String remotePath);
    
    /**
     * 开始下载文件
     * @param device 源设备
     * @param remotePath 远程文件路径
     * @param localPath 本地保存路径
     * @return 传输任务ID
     */
    String startDownload(Device device, String remotePath, String localPath);
    
    /**
     * 恢复下载（断点续传）
     */
    String resumeDownload(Device device, String remotePath, String localPath);
    
    /**
     * 获取传输进度
     */
    TransferInfo getProgress(String transferId);
    
    /**
     * 获取所有活跃的传输任务
     */
    Map<String, TransferInfo> getAllActiveTransfers();
    
    /**
     * 取消/暂停传输
     */
    void cancelTransfer(String transferId);
}
