package com.keke.ssh.application.assembler;

import com.keke.ssh.application.dto.SshTaskOutputDTO;
import com.keke.ssh.application.dto.SshTaskResultDTO;
import com.keke.ssh.domain.entity.SshTask;
import com.keke.ssh.domain.entity.SshTaskResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SSH任务装配器
 * 
 * DDD概念：装配器（Assembler）
 * - 负责领域对象与DTO之间的转换
 * - 保持应用服务的职责单一
 */
public final class SshTaskAssembler {
    
    private SshTaskAssembler() {
        // 工具类，禁止实例化
    }
    
    /**
     * 任务转换为输出DTO
     */
    public static SshTaskOutputDTO toOutputDTO(SshTask task, List<SshTaskResult> results) {
        List<SshTaskResultDTO> resultDTOs = results != null 
            ? results.stream().map(SshTaskAssembler::toResultDTO).collect(Collectors.toList())
            : List.of();
        
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
    
    /**
     * 结果转换为DTO
     */
    public static SshTaskResultDTO toResultDTO(SshTaskResult result) {
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
    
    /**
     * 批量转换任务
     */
    public static List<SshTaskOutputDTO> toOutputDTOList(List<SshTask> tasks, 
            java.util.function.Function<Long, List<SshTaskResult>> resultsFetcher) {
        return tasks.stream()
            .map(task -> toOutputDTO(task, resultsFetcher.apply(task.getId())))
            .collect(Collectors.toList());
    }
}
