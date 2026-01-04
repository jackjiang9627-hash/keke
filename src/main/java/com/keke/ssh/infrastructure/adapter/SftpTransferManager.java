package com.keke.ssh.infrastructure.adapter;

import com.keke.ssh.domain.entity.Device;
import com.keke.ssh.domain.valueobject.TransferProgress;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SFTP文件传输管理器 - 支持进度显示和断点续传
 */
@Slf4j
@Component
public class SftpTransferManager {
    
    private static final int BUFFER_SIZE = 32 * 1024; // 32KB buffer
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int AUTH_TIMEOUT_SECONDS = 30;
    
    private final Map<String, TransferProgress> activeTransfers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> cancelFlags = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * 异步上传文件，支持进度追踪
     */
    public String startUpload(Device device, byte[] fileContent,
                              String fileName, String remotePath) {
        String transferId = UUID.randomUUID().toString();
        
        TransferProgress progress = TransferProgress.builder()
                .transferId(transferId)
                .deviceId(device.getId())
                .deviceName(device.getName())
                .deviceHost(device.getHost())
                .fileName(fileName)
                .totalBytes(fileContent.length)
                .transferredBytes(0)
                .percentage(0)
                .status("PENDING")
                .startTime(System.currentTimeMillis())
                .resumable(true)
                .build();
        
        activeTransfers.put(transferId, progress);
        cancelFlags.put(transferId, false);
        
        executor.submit(() -> executeUpload(device, fileContent, remotePath, fileName, progress));
        
        return transferId;
    }
    
    /**
     * 异步下载文件，支持进度追踪和断点续传
     */
    public String startDownload(Device device, String remotePath, String localPath) {
        String transferId = UUID.randomUUID().toString();
        String fileName = Path.of(remotePath).getFileName().toString();
        
        TransferProgress progress = TransferProgress.builder()
                .transferId(transferId)
                .deviceId(device.getId())
                .deviceName(device.getName())
                .deviceHost(device.getHost())
                .fileName(fileName)
                .totalBytes(0)
                .transferredBytes(0)
                .percentage(0)
                .status("PENDING")
                .startTime(System.currentTimeMillis())
                .resumable(true)
                .build();
        
        activeTransfers.put(transferId, progress);
        cancelFlags.put(transferId, false);
        
        executor.submit(() -> executeDownload(device, remotePath, localPath, progress));
        
        return transferId;
    }
    
    private void executeUpload(Device device, byte[] fileContent, String remotePath, 
                               String fileName, TransferProgress progress) {
        progress.setStatus("RUNNING");
        progress.setStartTime(System.currentTimeMillis());
        progress.addLog("开始上传文件: " + fileName);
        progress.addLog("文件大小: " + formatBytes(fileContent.length));
        progress.addLog("目标设备: " + device.getUsername() + "@" + device.getHost() + ":" + device.getPort());
        
        log.info("开始上传文件: {} -> {}@{}:{}", fileName, device.getUsername(), device.getHost(), remotePath);
        
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            progress.addLog("正在连接SSH服务器...");
            log.debug("SSH客户端已启动");
            
            try (ClientSession session = client.connect(device.getUsername(), device.getHost(), device.getPort())
                    .verify(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                    .getSession()) {
                
                progress.addLog("SSH连接成功，正在认证...");
                // 认证
                log.debug("开始SSH认证: {}@{}:{}", device.getUsername(), device.getHost(), device.getPort());
                if (device.getPassword() != null) {
                    session.addPasswordIdentity(device.getPassword());
                }
                session.auth().verify(Duration.ofSeconds(AUTH_TIMEOUT_SECONDS));
                progress.addLog("SSH认证成功");
                log.info("SSH认证成功");
                
                // 创建SFTP客户端
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    progress.addLog("SFTP通道已建立");
                    log.debug("SFTP客户端已创建");
                    
                    // 构建完整的远程文件路径
                    String remoteFile;
                    if (remotePath.endsWith("/")) {
                        remoteFile = remotePath + fileName;
                    } else {
                        // 检查 remotePath 是否是目录
                        try {
                            SftpClient.Attributes attrs = sftp.stat(remotePath);
                            if (attrs.isDirectory()) {
                                remoteFile = remotePath + "/" + fileName;
                                log.debug("远程路径是目录，拼接文件名: {}", remoteFile);
                            } else {
                                // 是文件路径，直接使用
                                remoteFile = remotePath;
                            }
                        } catch (Exception e) {
                            // 路径不存在，假设是文件路径
                            remoteFile = remotePath;
                        }
                    }
                    
                    progress.addLog("目标路径: " + remoteFile);
                    log.info("目标远程文件路径: {}", remoteFile);
                    
                    // 确保远程目录存在
                    progress.addLog("检查并创建远程目录...");
                    log.debug("开始检查并创建远程目录");
                    ensureRemoteDirectory(sftp, remoteFile);
                    progress.addLog("远程目录已就绪");
                    log.info("远程目录已就绪");
                    
                    // 检查远程空间是否足够
                    long requiredSpace = progress.getTotalBytes();
                    if (requiredSpace > 0) {
                        progress.addLog("检查远程磁盘空间...");
                        log.debug("开始检查远程空间，需要: {}", formatBytes(requiredSpace));
                        String checkSpaceResult = checkRemoteSpace(session, remotePath, requiredSpace);
                        if (checkSpaceResult != null) {
                            progress.setStatus("FAILED");
                            progress.setErrorMessage(checkSpaceResult);
                            progress.addLog("失败: " + checkSpaceResult);
                            log.error("远程空间不足: {} - {}", remoteFile, checkSpaceResult);
                            return;
                        }
                        progress.addLog("远程空间检查通过");
                    }
                    
                    progress.addLog("开始传输数据...");
                    log.info("准备写入远程文件: {}", remoteFile);
                    try (OutputStream remoteOut = sftp.write(remoteFile, 
                            SftpClient.OpenMode.Create, SftpClient.OpenMode.Write, SftpClient.OpenMode.Truncate)) {
                        
                        log.info("开始传输数据，缓冲区大小: {} KB", BUFFER_SIZE / 1024);
                        long transferred = 0;
                        int offset = 0;
                        int totalLength = fileContent.length;
                        long lastLogTime = System.currentTimeMillis();
                        int lastLogPercentage = 0;
                        
                        while (offset < totalLength) {
                            // 检查是否取消
                            if (Boolean.TRUE.equals(cancelFlags.get(progress.getTransferId()))) {
                                progress.setStatus("CANCELLED");
                                progress.addLog("传输已取消");
                                log.info("上传已取消: {}", fileName);
                                return;
                            }
                            
                            int bytesToWrite = Math.min(BUFFER_SIZE, totalLength - offset);
                            remoteOut.write(fileContent, offset, bytesToWrite);
                            offset += bytesToWrite;
                            transferred += bytesToWrite;
                            progress.updateProgress(transferred);
                            
                            // 每25%记录一次进度日志
                            int currentPercentage = progress.getPercentage();
                            if (currentPercentage >= lastLogPercentage + 25) {
                                progress.addLog("传输进度: " + currentPercentage + "%");
                                lastLogPercentage = (currentPercentage / 25) * 25;
                            }
                            
                            // 每5秒打印一次进度日志
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastLogTime > 5000) {
                                log.info("上传进度: {} / {} ({}%)", 
                                        formatBytes(transferred), 
                                        formatBytes(progress.getTotalBytes()),
                                        progress.getPercentage());
                                lastLogTime = currentTime;
                            }
                        }
                        
                        progress.setStatus("COMPLETED");
                        progress.setPercentage(100);
                        progress.addLog("文件上传完成！");
                        log.info("文件上传完成: {} -> {}@{}:{}", 
                                fileName, device.getUsername(), device.getHost(), remoteFile);
                    }
                }
            }
        } catch (Exception e) {
            progress.setStatus("FAILED");
            progress.setErrorMessage(e.getMessage());
            progress.addLog("上传失败: " + e.getMessage());
            log.error("文件上传失败: {}", fileName, e);
        }
    }
    
    private void executeDownload(Device device, String remotePath, String localPath, 
                                 TransferProgress progress) {
        progress.setStatus("RUNNING");
        progress.setStartTime(System.currentTimeMillis());
        
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            
            try (ClientSession session = client.connect(device.getUsername(), device.getHost(), device.getPort())
                    .verify(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                    .getSession()) {
                
                // 认证
                if (device.getPassword() != null) {
                    session.addPasswordIdentity(device.getPassword());
                }
                session.auth().verify(Duration.ofSeconds(AUTH_TIMEOUT_SECONDS));
                
                // 创建SFTP客户端
                try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                    // 获取远程文件大小
                    SftpClient.Attributes attrs = sftp.stat(remotePath);
                    long remoteSize = attrs.getSize();
                    progress.setTotalBytes(remoteSize);
                    
                    Path localFilePath = Path.of(localPath);
                    long existingSize = 0;
                    
                    // 检查是否需要断点续传
                    if (Files.exists(localFilePath)) {
                        existingSize = Files.size(localFilePath);
                        if (existingSize < remoteSize) {
                            log.info("检测到部分下载文件，从 {} 字节处继续", existingSize);
                            progress.setTransferredBytes(existingSize);
                        } else if (existingSize == remoteSize) {
                            progress.setStatus("COMPLETED");
                            progress.setPercentage(100);
                            log.info("文件已存在且大小一致，跳过下载: {}", localPath);
                            return;
                        }
                    } else {
                        // 创建父目录
                        Files.createDirectories(localFilePath.getParent());
                    }
                    
                    // 断点续传：从现有位置开始
                    try (InputStream remoteIn = sftp.read(remotePath);
                         OutputStream localOut = Files.newOutputStream(localFilePath, 
                                 StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        
                        // 跳过已下载的部分
                        if (existingSize > 0) {
                            long skipped = remoteIn.skip(existingSize);
                            log.info("跳过已下载部分: {} 字节", skipped);
                        }
                        
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long transferred = existingSize;
                        int bytesRead;
                        
                        while ((bytesRead = remoteIn.read(buffer)) != -1) {
                            // 检查是否取消/暂停
                            if (Boolean.TRUE.equals(cancelFlags.get(progress.getTransferId()))) {
                                progress.setStatus("PAUSED");
                                log.info("下载已暂停: {}, 已传输 {} 字节", progress.getFileName(), transferred);
                                return;
                            }
                            
                            localOut.write(buffer, 0, bytesRead);
                            transferred += bytesRead;
                            progress.updateProgress(transferred);
                        }
                        
                        progress.setStatus("COMPLETED");
                        progress.setPercentage(100);
                        log.info("文件下载完成: {}@{}:{} -> {}", 
                                device.getUsername(), device.getHost(), remotePath, localPath);
                    }
                }
            }
        } catch (Exception e) {
            progress.setStatus("FAILED");
            progress.setErrorMessage(e.getMessage());
            log.error("文件下载失败: {} - {}", remotePath, e.getMessage());
        }
    }
    
    /**
     * 获取传输进度
     */
    public TransferProgress getProgress(String transferId) {
        return activeTransfers.get(transferId);
    }
    
    /**
     * 获取所有活跃的传输任务
     */
    public Map<String, TransferProgress> getAllActiveTransfers() {
        return new ConcurrentHashMap<>(activeTransfers);
    }
    
    /**
     * 取消/暂停传输
     */
    public void cancelTransfer(String transferId) {
        cancelFlags.put(transferId, true);
    }
    
    /**
     * 恢复传输（断点续传）
     */
    public String resumeDownload(Device device, String remotePath, String localPath) {
        return startDownload(device, remotePath, localPath);
    }
    
    /**
     * 清理已完成的传输记录
     */
    public void cleanupCompleted() {
        activeTransfers.entrySet().removeIf(entry -> {
            String status = entry.getValue().getStatus();
            return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
        });
    }
    
    /**
     * 确保远程目录存在
     */
    private void ensureRemoteDirectory(SftpClient sftp, String remoteFilePath) throws IOException {
        log.debug("检查远程目录: {}", remoteFilePath);
        try {
            // 从文件路径提取目录路径
            String remoteDir;
            if (remoteFilePath.contains("/")) {
                remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
            } else {
                log.debug("文件在当前目录，无需创建目录");
                return; // 当前目录，无需创建
            }
            
            if (remoteDir.isEmpty()) {
                log.debug("文件在根目录，无需创建目录");
                return; // 根目录
            }
            
            log.info("需要确保目录存在: {}", remoteDir);
            
            // 递归创建目录
            String[] dirs = remoteDir.split("/");
            StringBuilder currentPath = new StringBuilder();
            
            for (String dir : dirs) {
                if (dir.isEmpty()) {
                    currentPath.append("/");
                    continue;
                }
                
                if (currentPath.length() > 0 && currentPath.charAt(currentPath.length() - 1) != '/') {
                    currentPath.append("/");
                }
                currentPath.append(dir);
                
                try {
                    // 尝试获取目录信息，如果不存在会抛出异常
                    sftp.stat(currentPath.toString());
                    log.debug("目录已存在: {}", currentPath);
                } catch (Exception e) {
                    // 目录不存在，创建它
                    log.info("目录不存在，开始创建: {}", currentPath);
                    try {
                        sftp.mkdir(currentPath.toString());
                        log.info("创建远程目录成功: {}", currentPath);
                    } catch (Exception mkdirEx) {
                        // 目录可能已经存在（并发情况），忽略错误
                        log.debug("创建目录失败（可能已存在）: {} - {}", currentPath, mkdirEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("确保远程目录存在失败: {}", remoteFilePath, e);
            throw new IOException("无法创建远程目录: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查远程空间是否足够
     * @return null表示空间足够，否则返回错误信息
     */
    private String checkRemoteSpace(ClientSession session, String remotePath, long requiredBytes) {
        try {
            // 获取远程目录
            String dir = remotePath.endsWith("/") ? remotePath : 
                    remotePath.substring(0, remotePath.lastIndexOf('/') + 1);
            if (dir.isEmpty()) dir = "/";
            
            // 执行df命令检查磁盘空间
            try (org.apache.sshd.client.channel.ChannelExec channel = session.createExecChannel(
                    "df -B1 " + dir + " 2>/dev/null | tail -1 | awk '{print $4}'")) {
                
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                channel.setOut(outputStream);
                channel.open().verify(Duration.ofSeconds(10));
                channel.waitFor(java.util.EnumSet.of(
                        org.apache.sshd.client.channel.ClientChannelEvent.CLOSED), 10000);
                
                String output = outputStream.toString().trim();
                if (!output.isEmpty()) {
                    try {
                        long availableBytes = Long.parseLong(output);
                        if (availableBytes < requiredBytes) {
                            return String.format("远程空间不足: 需要 %s，可用 %s",
                                    formatBytes(requiredBytes), formatBytes(availableBytes));
                        }
                        log.info("远程空间检查通过: 需要 {}，可用 {}", 
                                formatBytes(requiredBytes), formatBytes(availableBytes));
                    } catch (NumberFormatException e) {
                        log.warn("无法解析远程空间: {}", output);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检查远程空间失败: {}", e.getMessage());
        }
        return null;  // 默认允许传输
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
