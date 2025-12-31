package com.loganalyzer.monitor.application.assembler;

import com.loganalyzer.monitor.application.dto.MonitorSnapshotDTO;
import com.loganalyzer.monitor.application.dto.MonitorTaskDTO;
import com.loganalyzer.monitor.domain.entity.MonitorSnapshot;
import com.loganalyzer.monitor.domain.entity.MonitorTask;
import com.loganalyzer.monitor.domain.valueobject.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控数据组装器
 */
@Component
public class MonitorAssembler {
    
    /**
     * 将领域实体转换为DTO
     */
    public MonitorSnapshotDTO toDTO(MonitorSnapshot snapshot) {
        if (snapshot == null) return null;
        
        MonitorSnapshotDTO.OverviewDTO overview = MonitorSnapshotDTO.OverviewDTO.builder()
                .cpuUsage(snapshot.getCpuInfo() != null ? snapshot.getCpuInfo().getUsagePercent() : 0)
                .memoryUsage(snapshot.getMemoryInfo() != null ? snapshot.getMemoryInfo().getUsagePercent() : 0)
                .diskUsage(snapshot.getTotalDiskUsage())
                .processCount(snapshot.getProcessInfoList() != null ? snapshot.getProcessInfoList().size() : 0)
                .networkInterfaceCount(snapshot.getNetworkInfoList() != null ? snapshot.getNetworkInfoList().size() : 0)
                .build();
        
        return MonitorSnapshotDTO.builder()
                .collectTime(snapshot.getCollectTime())
                .healthStatus(snapshot.getHealthStatus().getDescription())
                .systemInfo(toSystemInfoDTO(snapshot.getSystemInfo()))
                .cpuInfo(toCpuInfoDTO(snapshot.getCpuInfo()))
                .memoryInfo(toMemoryInfoDTO(snapshot.getMemoryInfo()))
                .diskInfoList(toDiskInfoDTOList(snapshot.getDiskInfoList()))
                .networkInfoList(toNetworkInfoDTOList(snapshot.getNetworkInfoList()))
                .processInfoList(toProcessInfoDTOList(snapshot.getProcessInfoList()))
                .overview(overview)
                .build();
    }
    
    private MonitorSnapshotDTO.SystemInfoDTO toSystemInfoDTO(SystemInfo info) {
        if (info == null) return null;
        return MonitorSnapshotDTO.SystemInfoDTO.builder()
                .hostname(info.getHostname())
                .osName(info.getOsName())
                .osVersion(info.getOsVersion())
                .osArch(info.getOsArch())
                .upTime(info.formatUpTime())
                .currentUser(info.getCurrentUser())
                .javaVersion(info.getJavaVersion())
                .jvmMemory(formatBytes(info.getJvmUsedMemory()) + " / " + formatBytes(info.getJvmMaxMemory()))
                .build();
    }
    
    private MonitorSnapshotDTO.CpuInfoDTO toCpuInfoDTO(CpuInfo info) {
        if (info == null) return null;
        return MonitorSnapshotDTO.CpuInfoDTO.builder()
                .model(info.getModel())
                .physicalCores(info.getPhysicalCores())
                .logicalCores(info.getLogicalCores())
                .usagePercent(roundTo2(info.getUsagePercent()))
                .systemPercent(roundTo2(info.getSystemPercent()))
                .userPercent(roundTo2(info.getUserPercent()))
                .idlePercent(roundTo2(info.getIdlePercent()))
                .temperature(roundTo2(info.getTemperature()))
                .frequency(info.getFrequency())
                .build();
    }
    
    private MonitorSnapshotDTO.MemoryInfoDTO toMemoryInfoDTO(MemoryInfo info) {
        if (info == null) return null;
        return MonitorSnapshotDTO.MemoryInfoDTO.builder()
                .total(info.formatTotal())
                .used(info.formatUsed())
                .available(info.formatAvailable())
                .usagePercent(roundTo2(info.getUsagePercent()))
                .swapTotal(formatBytes(info.getSwapTotal()))
                .swapUsed(formatBytes(info.getSwapUsed()))
                .swapUsagePercent(roundTo2(info.getSwapUsagePercent()))
                .totalBytes(info.getTotal())
                .usedBytes(info.getUsed())
                .build();
    }
    
    private List<MonitorSnapshotDTO.DiskInfoDTO> toDiskInfoDTOList(List<DiskInfo> list) {
        if (list == null) return null;
        return list.stream().map(this::toDiskInfoDTO).collect(Collectors.toList());
    }
    
    private MonitorSnapshotDTO.DiskInfoDTO toDiskInfoDTO(DiskInfo info) {
        return MonitorSnapshotDTO.DiskInfoDTO.builder()
                .name(info.getName())
                .type(info.getType())
                .total(info.formatTotal())
                .used(info.formatUsed())
                .available(info.formatAvailable())
                .usagePercent(roundTo2(info.getUsagePercent()))
                .readSpeed(formatBytes(info.getReadSpeed()) + "/s")
                .writeSpeed(formatBytes(info.getWriteSpeed()) + "/s")
                .build();
    }
    
    private List<MonitorSnapshotDTO.NetworkInfoDTO> toNetworkInfoDTOList(List<NetworkInfo> list) {
        if (list == null) return null;
        return list.stream().map(this::toNetworkInfoDTO).collect(Collectors.toList());
    }
    
    private MonitorSnapshotDTO.NetworkInfoDTO toNetworkInfoDTO(NetworkInfo info) {
        return MonitorSnapshotDTO.NetworkInfoDTO.builder()
                .name(info.getName())
                .displayName(info.getDisplayName())
                .macAddress(info.getMacAddress())
                .ipv4Address(info.getIpv4Address())
                .bytesReceived(formatBytes(info.getBytesReceived()))
                .bytesSent(formatBytes(info.getBytesSent()))
                .receiveSpeed(info.formatReceiveSpeed())
                .sendSpeed(info.formatSendSpeed())
                .speed(info.formatSpeed())
                .connected(info.isConnected())
                .build();
    }
    
    private List<MonitorSnapshotDTO.ProcessInfoDTO> toProcessInfoDTOList(List<ProcessInfo> list) {
        if (list == null) return null;
        return list.stream().map(this::toProcessInfoDTO).collect(Collectors.toList());
    }
    
    private MonitorSnapshotDTO.ProcessInfoDTO toProcessInfoDTO(ProcessInfo info) {
        return MonitorSnapshotDTO.ProcessInfoDTO.builder()
                .pid(info.getPid())
                .name(info.getName())
                .state(info.getState())
                .cpuPercent(roundTo2(info.getCpuPercent()))
                .memoryPercent(roundTo2(info.getMemoryPercent()))
                .memory(info.formatResidentMemory())
                .threadCount(info.getThreadCount())
                .user(info.getUser())
                .build();
    }
    
    /**
     * 任务实体转DTO
     */
    public MonitorTaskDTO toTaskDTO(MonitorTask task) {
        if (task == null) return null;
        return MonitorTaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .type(task.getType().name())
                .typeDescription(task.getType().getDescription())
                .status(task.getStatus().name())
                .statusDescription(task.getStatus().getDescription())
                .intervalSeconds(task.getIntervalSeconds())
                .maxExecuteCount(task.getMaxExecuteCount())
                .createTime(task.getCreateTime())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .nextExecuteTime(task.getNextExecuteTime())
                .executeCount(task.getExecuteCount())
                .lastResult(task.getLastResult())
                .remark(task.getRemark())
                .build();
    }
    
    private double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
