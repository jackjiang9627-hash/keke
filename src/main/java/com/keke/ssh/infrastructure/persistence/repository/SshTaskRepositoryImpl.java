package com.keke.ssh.infrastructure.persistence.repository;

import com.keke.ssh.domain.entity.SshTask;
import com.keke.ssh.domain.entity.SshTaskResult;
import com.keke.ssh.domain.repository.SshTaskRepository;
import com.keke.ssh.domain.valueobject.SshTaskStatus;
import com.keke.ssh.domain.valueobject.SshTaskType;
import com.keke.ssh.infrastructure.persistence.entity.SshTaskEntity;
import com.keke.ssh.infrastructure.persistence.entity.SshTaskResultEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SshTaskRepositoryImpl implements SshTaskRepository {
    
    private final SshTaskJpaRepository taskJpaRepository;
    private final SshTaskResultJpaRepository resultJpaRepository;
    
    @Override
    public SshTask save(SshTask task) {
        SshTaskEntity entity = toEntity(task);
        SshTaskEntity saved = taskJpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<SshTask> findById(Long id) {
        return taskJpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public List<SshTask> findAll() {
        return taskJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<SshTask> findRecent(int limit) {
        return taskJpaRepository.findTop50ByOrderByCreateTimeDesc().stream()
                .limit(limit)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        taskJpaRepository.deleteById(id);
    }
    
    @Override
    public SshTaskResult saveResult(SshTaskResult result) {
        SshTaskResultEntity entity = toResultEntity(result);
        SshTaskResultEntity saved = resultJpaRepository.save(entity);
        return toResultDomain(saved);
    }
    
    @Override
    public List<SshTaskResult> findResultsByTaskId(Long taskId) {
        return resultJpaRepository.findByTaskIdOrderByIdAsc(taskId).stream()
                .map(this::toResultDomain)
                .collect(Collectors.toList());
    }
    
    private SshTaskEntity toEntity(SshTask task) {
        String deviceIdsStr = task.getDeviceIds() != null 
                ? task.getDeviceIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : "";
        return SshTaskEntity.builder()
                .id(task.getId())
                .name(task.getName())
                .type(task.getType() != null ? task.getType().name() : null)
                .command(task.getCommand())
                .localPath(task.getLocalPath())
                .remotePath(task.getRemotePath())
                .deviceIds(deviceIdsStr)
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .createTime(task.getCreateTime())
                .createdBy(task.getCreatedBy())
                .build();
    }
    
    private SshTask toDomain(SshTaskEntity entity) {
        List<Long> deviceIds = new ArrayList<>();
        if (entity.getDeviceIds() != null && !entity.getDeviceIds().isEmpty()) {
            deviceIds = Arrays.stream(entity.getDeviceIds().split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        return SshTask.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType() != null ? SshTaskType.valueOf(entity.getType()) : null)
                .command(entity.getCommand())
                .localPath(entity.getLocalPath())
                .remotePath(entity.getRemotePath())
                .deviceIds(deviceIds)
                .status(entity.getStatus() != null ? SshTaskStatus.valueOf(entity.getStatus()) : null)
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createTime(entity.getCreateTime())
                .createdBy(entity.getCreatedBy())
                .build();
    }
    
    private SshTaskResultEntity toResultEntity(SshTaskResult result) {
        return SshTaskResultEntity.builder()
                .id(result.getId())
                .taskId(result.getTaskId())
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
    
    private SshTaskResult toResultDomain(SshTaskResultEntity entity) {
        return SshTaskResult.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .deviceId(entity.getDeviceId())
                .deviceName(entity.getDeviceName())
                .deviceHost(entity.getDeviceHost())
                .status(entity.getStatus() != null ? SshTaskStatus.valueOf(entity.getStatus()) : null)
                .output(entity.getOutput())
                .errorMessage(entity.getErrorMessage())
                .exitCode(entity.getExitCode())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .durationMs(entity.getDurationMs())
                .build();
    }
}
