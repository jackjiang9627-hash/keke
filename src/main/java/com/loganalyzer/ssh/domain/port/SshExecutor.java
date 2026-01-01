package com.loganalyzer.ssh.domain.port;

import com.loganalyzer.ssh.domain.entity.Device;

/**
 * SSH执行端口 - 抽象SSH操作
 */
public interface SshExecutor {
    
    /**
     * 测试SSH连接
     * @param device 目标设备
     * @return 连接结果（成功返回null，失败返回错误信息）
     */
    String testConnection(Device device);
    
    /**
     * 执行命令
     * @param device 目标设备
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    CommandResult executeCommand(Device device, String command);
    
    /**
     * 上传文件
     * @param device 目标设备
     * @param localPath 本地文件路径
     * @param remotePath 远程文件路径
     * @return 上传结果（成功返回null，失败返回错误信息）
     */
    String uploadFile(Device device, String localPath, String remotePath);
    
    /**
     * 下载文件
     * @param device 目标设备
     * @param remotePath 远程文件路径
     * @param localPath 本地保存路径
     * @return 下载结果（成功返回null，失败返回错误信息）
     */
    String downloadFile(Device device, String remotePath, String localPath);
    
    /**
     * 命令执行结果
     */
    record CommandResult(
        boolean success,
        String output,
        String error,
        int exitCode
    ) {
        public static CommandResult success(String output, int exitCode) {
            return new CommandResult(true, output, null, exitCode);
        }
        
        public static CommandResult failure(String error) {
            return new CommandResult(false, null, error, -1);
        }
    }
}
