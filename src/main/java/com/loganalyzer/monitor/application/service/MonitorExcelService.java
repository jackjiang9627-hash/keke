package com.loganalyzer.monitor.application.service;

import com.loganalyzer.monitor.application.dto.MonitorSnapshotDTO;
import com.loganalyzer.monitor.application.dto.MonitorTaskDTO;
import com.loganalyzer.shared.infrastructure.excel.ExcelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 监控报告Excel导出服务
 * 使用ExcelBuilder工具类重构
 */
@Slf4j
@Service
public class MonitorExcelService {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 导出监控报告
     */
    public byte[] exportReport(MonitorSnapshotDTO snapshot) throws IOException {
        ExcelBuilder builder = ExcelBuilder.create();
        
        // 系统概览Sheet
        buildOverviewSheet(builder, snapshot);
        
        // CPU Sheet
        buildCpuSheet(builder, snapshot);
        
        // 内存Sheet
        buildMemorySheet(builder, snapshot);
        
        // 磁盘Sheet
        buildDiskSheet(builder, snapshot);
        
        // 网络Sheet
        buildNetworkSheet(builder, snapshot);
        
        // 进程Sheet
        buildProcessSheet(builder, snapshot);
        
        return builder.build();
    }
    
    /**
     * 导出历史报告
     */
    public byte[] exportHistoryReport(List<MonitorSnapshotDTO> snapshots) throws IOException {
        return ExcelBuilder.create()
                .sheet("监控历史")
                    .headers("采集时间", "健康状态", "CPU使用率(%)", "内存使用率(%)", "磁盘使用率(%)", "进程数")
                    .columnWidths(6000, 4000, 5000, 5000, 5000, 4000)
                    .data(snapshots, s -> new Object[]{
                            s.getCollectTime().format(DATE_FORMAT),
                            s.getHealthStatus(),
                            s.getOverview().getCpuUsage(),
                            s.getOverview().getMemoryUsage(),
                            s.getOverview().getDiskUsage(),
                            s.getOverview().getProcessCount()
                    })
                .build();
    }
    
    /**
     * 批量导出任务报告（每个任务一个Sheet，包含所有监控数据）
     * @param tasks 任务列表
     * @param taskSnapshotsList 每个任务对应的快照历史列表
     */
    public byte[] exportTaskReports(List<MonitorTaskDTO> tasks, List<List<MonitorSnapshotDTO>> taskSnapshotsList) throws IOException {
        ExcelBuilder builder = ExcelBuilder.create();
        
        // 为每个任务创建一个Sheet
        for (int i = 0; i < tasks.size(); i++) {
            MonitorTaskDTO task = tasks.get(i);
            List<MonitorSnapshotDTO> snapshots = i < taskSnapshotsList.size() ? taskSnapshotsList.get(i) : List.of();
            
            // 使用任务ID确保Sheet名称唯一
            String sheetName = task.getName() + "_" + task.getId();
            // Excel Sheet名称限制：最长31个字符
            if (sheetName.length() > 31) {
                sheetName = sheetName.substring(0, 26) + "_" + task.getId();
                if (sheetName.length() > 31) {
                    sheetName = "任务_" + task.getId();
                }
            }
            
            ExcelBuilder.SheetBuilder sheet = builder.sheet(sheetName);
            
            // 任务基本信息
            sheet.title("任务信息")
                    .keyValue("任务ID", task.getId().toString())
                    .keyValue("任务名称", task.getName())
                    .keyValue("任务类型", task.getTypeDescription())
                    .keyValue("任务状态", task.getStatusDescription())
                    .keyValue("检测间隔", task.getIntervalSeconds() != null ? task.getIntervalSeconds() + "秒" : "N/A")
                    .keyValue("创建时间", task.getCreateTime() != null ? task.getCreateTime().format(DATE_FORMAT) : "N/A")
                    .keyValue("启动时间", task.getStartTime() != null ? task.getStartTime().format(DATE_FORMAT) : "N/A")
                    .keyValue("执行次数", task.getExecuteCount().toString())
                    .keyValue("最后结果", task.getLastResult() != null ? task.getLastResult() : "N/A")
                    .emptyRow();
            
            // 监控数据历史表格
            if (!snapshots.isEmpty()) {
                sheet.section("监控数据历史 (共" + snapshots.size() + "条记录)")
                        .emptyRow()
                        .headers("采集时间", "健康状态", "CPU使用率(%)", "内存使用率(%)", "磁盘使用率(%)", "进程数")
                        .columnWidths(6000, 4000, 5000, 5000, 5000, 4000)
                        .data(snapshots, s -> new Object[]{
                                s.getCollectTime().format(DATE_FORMAT),
                                s.getHealthStatus(),
                                s.getOverview() != null ? String.format("%.2f", s.getOverview().getCpuUsage()) : "N/A",
                                s.getOverview() != null ? String.format("%.2f", s.getOverview().getMemoryUsage()) : "N/A",
                                s.getOverview() != null ? String.format("%.2f", s.getOverview().getDiskUsage()) : "N/A",
                                s.getOverview() != null ? s.getOverview().getProcessCount() : 0
                        });
            } else {
                sheet.section("提示")
                        .keyValue("说明", "该任务尚未执行或没有监控数据");
            }
        }
        
        return builder.build();
    }
    
    private void buildOverviewSheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        ExcelBuilder.SheetBuilder sheet = builder.sheet("系统概览")
                .title("系统监控报告")
                .keyValue("采集时间", snapshot.getCollectTime().format(DATE_FORMAT))
                .keyValue("健康状态", snapshot.getHealthStatus())
                .emptyRow();
        
        // 系统信息
        if (snapshot.getSystemInfo() != null) {
            MonitorSnapshotDTO.SystemInfoDTO sys = snapshot.getSystemInfo();
            sheet.section("系统信息")
                    .keyValue("主机名", sys.getHostname())
                    .keyValue("操作系统", sys.getOsName() + " " + sys.getOsVersion())
                    .keyValue("系统架构", sys.getOsArch())
                    .keyValue("运行时长", sys.getUpTime())
                    .keyValue("当前用户", sys.getCurrentUser())
                    .keyValue("Java版本", sys.getJavaVersion())
                    .keyValue("JVM内存", sys.getJvmMemory())
                    .emptyRow();
        }
        
        // 资源使用概览
        MonitorSnapshotDTO.OverviewDTO overview = snapshot.getOverview();
        sheet.section("资源使用概览")
                .keyValue("CPU使用率", String.format("%.2f%%", overview.getCpuUsage()))
                .keyValue("内存使用率", String.format("%.2f%%", overview.getMemoryUsage()))
                .keyValue("磁盘使用率", String.format("%.2f%%", overview.getDiskUsage()))
                .keyValue("进程数", overview.getProcessCount())
                .keyValue("网卡数", overview.getNetworkInterfaceCount());
    }
    
    private void buildCpuSheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        if (snapshot.getCpuInfo() == null) return;
        
        MonitorSnapshotDTO.CpuInfoDTO cpu = snapshot.getCpuInfo();
        builder.sheet("CPU信息")
                .section("CPU详细信息")
                .emptyRow()
                .keyValue("型号", cpu.getModel())
                .keyValue("物理核心数", cpu.getPhysicalCores())
                .keyValue("逻辑核心数", cpu.getLogicalCores())
                .keyValue("总使用率", String.format("%.2f%%", cpu.getUsagePercent()))
                .keyValue("系统占用", String.format("%.2f%%", cpu.getSystemPercent()))
                .keyValue("用户占用", String.format("%.2f%%", cpu.getUserPercent()))
                .keyValue("空闲率", String.format("%.2f%%", cpu.getIdlePercent()))
                .keyValue("温度", cpu.getTemperature() > 0 ? String.format("%.1f°C", cpu.getTemperature()) : "N/A")
                .keyValue("频率", cpu.getFrequency() + " MHz");
    }
    
    private void buildMemorySheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        if (snapshot.getMemoryInfo() == null) return;
        
        MonitorSnapshotDTO.MemoryInfoDTO mem = snapshot.getMemoryInfo();
        builder.sheet("内存信息")
                .section("内存详细信息")
                .emptyRow()
                .keyValue("总内存", mem.getTotal())
                .keyValue("已使用", mem.getUsed())
                .keyValue("可用", mem.getAvailable())
                .keyValue("使用率", String.format("%.2f%%", mem.getUsagePercent()))
                .emptyRow()
                .section("交换区信息")
                .keyValue("交换区总量", mem.getSwapTotal())
                .keyValue("交换区已使用", mem.getSwapUsed())
                .keyValue("交换区使用率", String.format("%.2f%%", mem.getSwapUsagePercent()));
    }
    
    private void buildDiskSheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        if (snapshot.getDiskInfoList() == null || snapshot.getDiskInfoList().isEmpty()) return;
        
        builder.sheet("磁盘信息")
                .headers("挂载点", "类型", "总容量", "已使用", "可用", "使用率", "读速度", "写速度")
                .columnWidths(4000, 3000, 4000, 4000, 4000, 4000, 4000, 4000)
                .data(snapshot.getDiskInfoList(), d -> new Object[]{
                        d.getName(),
                        d.getType(),
                        d.getTotal(),
                        d.getUsed(),
                        d.getAvailable(),
                        String.format("%.2f%%", d.getUsagePercent()),
                        d.getReadSpeed(),
                        d.getWriteSpeed()
                });
    }
    
    private void buildNetworkSheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        if (snapshot.getNetworkInfoList() == null || snapshot.getNetworkInfoList().isEmpty()) return;
        
        builder.sheet("网络信息")
                .headers("网卡名称", "IP地址", "MAC地址", "接收速度", "发送速度", "带宽", "状态")
                .columnWidths(5000, 5000, 5000, 4000, 4000, 4000, 3000)
                .data(snapshot.getNetworkInfoList(), n -> new Object[]{
                        n.getDisplayName(),
                        n.getIpv4Address(),
                        n.getMacAddress(),
                        n.getReceiveSpeed(),
                        n.getSendSpeed(),
                        n.getSpeed(),
                        n.isConnected() ? "已连接" : "未连接"
                });
    }
    
    private void buildProcessSheet(ExcelBuilder builder, MonitorSnapshotDTO snapshot) {
        if (snapshot.getProcessInfoList() == null || snapshot.getProcessInfoList().isEmpty()) return;
        
        builder.sheet("进程信息")
                .headers("PID", "进程名", "状态", "CPU(%)", "内存(%)", "内存占用", "线程数", "用户")
                .columnWidths(3000, 5000, 3000, 3000, 3000, 4000, 3000, 4000)
                .data(snapshot.getProcessInfoList(), p -> new Object[]{
                        p.getPid(),
                        p.getName(),
                        p.getState(),
                        String.format("%.2f", p.getCpuPercent()),
                        String.format("%.2f", p.getMemoryPercent()),
                        p.getMemory(),
                        p.getThreadCount(),
                        p.getUser()
                });
    }
}
