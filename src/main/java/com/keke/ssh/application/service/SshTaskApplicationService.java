package com.keke.ssh.application.service;

import com.keke.ssh.application.dto.SshTaskInputDTO;
import com.keke.ssh.application.dto.SshTaskOutputDTO;
import com.keke.ssh.application.dto.SshTaskResultDTO;
import com.keke.ssh.domain.entity.Device;
import com.keke.ssh.domain.entity.SshTask;
import com.keke.ssh.domain.entity.SshTaskResult;
import com.keke.ssh.domain.port.SshExecutor;
import com.keke.ssh.domain.repository.DeviceRepository;
import com.keke.ssh.domain.repository.SshTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SSH任务应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SshTaskApplicationService {
    
    private final SshTaskRepository taskRepository;
    private final DeviceRepository deviceRepository;
    private final SshExecutor sshExecutor;
    
    /**
     * 执行命令任务（同步）
     */
    @Transactional
    public SshTaskOutputDTO executeCommand(SshTaskInputDTO input) {
        // 创建任务
        SshTask task = SshTask.createCommandTask(
                input.getName(),
                input.getCommand(),
                input.getDeviceIds()
        );
        task = taskRepository.save(task);
        task.start();
        task = taskRepository.save(task);
        
        // 获取目标设备
        List<Device> devices = deviceRepository.findByIds(input.getDeviceIds());
        
        // 在每个设备上执行命令
        List<SshTaskResult> results = new ArrayList<>();
        boolean allSuccess = true;
        
        for (Device device : devices) {
            SshTaskResult result = SshTaskResult.create(
                    task.getId(),
                    device.getId(),
                    device.getName(),
                    device.getHost()
            );
            result.start();
            
            try {
                SshExecutor.CommandResult cmdResult = sshExecutor.executeCommand(device, input.getCommand());
                if (cmdResult.success()) {
                    result.success(cmdResult.output(), cmdResult.exitCode());
                } else {
                    result.fail(cmdResult.error());
                    allSuccess = false;
                }
            } catch (Exception e) {
                result.fail(e.getMessage());
                allSuccess = false;
            }
            
            results.add(taskRepository.saveResult(result));
        }
        
        // 更新任务状态
        task.complete(allSuccess);
        task = taskRepository.save(task);
        
        log.info("命令任务执行完成: {} - {}", task.getName(), task.getStatus());
        return toOutputDTO(task, results);
    }
    
    /**
     * 执行文件上传任务
     */
    @Transactional
    public SshTaskOutputDTO executeUpload(SshTaskInputDTO input) {
        SshTask task = SshTask.createUploadTask(
                input.getName(),
                input.getLocalPath(),
                input.getRemotePath(),
                input.getDeviceIds()
        );
        task = taskRepository.save(task);
        task.start();
        task = taskRepository.save(task);
        
        List<Device> devices = deviceRepository.findByIds(input.getDeviceIds());
        List<SshTaskResult> results = new ArrayList<>();
        boolean allSuccess = true;
        
        for (Device device : devices) {
            SshTaskResult result = SshTaskResult.create(
                    task.getId(),
                    device.getId(),
                    device.getName(),
                    device.getHost()
            );
            result.start();
            
            try {
                String error = sshExecutor.uploadFile(device, input.getLocalPath(), input.getRemotePath());
                if (error == null) {
                    result.success("上传成功", 0);
                } else {
                    result.fail(error);
                    allSuccess = false;
                }
            } catch (Exception e) {
                result.fail(e.getMessage());
                allSuccess = false;
            }
            
            results.add(taskRepository.saveResult(result));
        }
        
        task.complete(allSuccess);
        task = taskRepository.save(task);
        
        log.info("上传任务执行完成: {} - {}", task.getName(), task.getStatus());
        return toOutputDTO(task, results);
    }
    
    /**
     * 执行文件下载任务
     */
    @Transactional
    public SshTaskOutputDTO executeDownload(SshTaskInputDTO input) {
        SshTask task = SshTask.createDownloadTask(
                input.getName(),
                input.getRemotePath(),
                input.getLocalPath(),
                input.getDeviceIds()
        );
        task = taskRepository.save(task);
        task.start();
        task = taskRepository.save(task);
        
        List<Device> devices = deviceRepository.findByIds(input.getDeviceIds());
        List<SshTaskResult> results = new ArrayList<>();
        boolean allSuccess = true;
        
        for (Device device : devices) {
            SshTaskResult result = SshTaskResult.create(
                    task.getId(),
                    device.getId(),
                    device.getName(),
                    device.getHost()
            );
            result.start();
            
            try {
                // 为每个设备生成唯一的本地保存路径
                String localPath = input.getLocalPath();
                if (devices.size() > 1) {
                    int lastDot = localPath.lastIndexOf('.');
                    if (lastDot > 0) {
                        localPath = localPath.substring(0, lastDot) + "_" + device.getHost() + localPath.substring(lastDot);
                    } else {
                        localPath = localPath + "_" + device.getHost();
                    }
                }
                
                String error = sshExecutor.downloadFile(device, input.getRemotePath(), localPath);
                if (error == null) {
                    result.success("下载成功: " + localPath, 0);
                } else {
                    result.fail(error);
                    allSuccess = false;
                }
            } catch (Exception e) {
                result.fail(e.getMessage());
                allSuccess = false;
            }
            
            results.add(taskRepository.saveResult(result));
        }
        
        task.complete(allSuccess);
        task = taskRepository.save(task);
        
        log.info("下载任务执行完成: {} - {}", task.getName(), task.getStatus());
        return toOutputDTO(task, results);
    }
    
    /**
     * 获取任务详情
     */
    public SshTaskOutputDTO getTask(Long id) {
        SshTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        List<SshTaskResult> results = taskRepository.findResultsByTaskId(id);
        return toOutputDTO(task, results);
    }
    
    /**
     * 获取最近任务列表
     */
    public List<SshTaskOutputDTO> getRecentTasks(int limit) {
        return taskRepository.findRecent(limit).stream()
                .map(task -> {
                    List<SshTaskResult> results = taskRepository.findResultsByTaskId(task.getId());
                    return toOutputDTO(task, results);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
        log.info("删除任务成功: {}", id);
    }
    
    private SshTaskOutputDTO toOutputDTO(SshTask task, List<SshTaskResult> results) {
        List<SshTaskResultDTO> resultDTOs = results.stream()
                .map(this::toResultDTO)
                .collect(Collectors.toList());
        
        return SshTaskOutputDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .type(task.getType() != null ? task.getType().name() : null)
                .command(task.getCommand())
                .localPath(task.getLocalPath())
                .remotePath(task.getRemotePath())
                .deviceCount(task.getDeviceIds() != null ? task.getDeviceIds().size() : 0)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .createTime(task.getCreateTime())
                .results(resultDTOs)
                .build();
    }
    
    private SshTaskResultDTO toResultDTO(SshTaskResult result) {
        return SshTaskResultDTO.builder()
                .id(result.getId())
                .deviceId(result.getDeviceId())
                .deviceName(result.getDeviceName())
                .deviceHost(result.getDeviceHost())
                .status(result.getStatus() != null ? result.getStatus().name() : null)
                .output(result.getOutput())
                .errorMessage(result.getErrorMessage())
                .exitCode(result.getExitCode())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .durationMs(result.getDurationMs())
                .build();
    }
}
