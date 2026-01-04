package com.keke.monitor.infrastructure.adapter;

import com.keke.monitor.domain.entity.MonitorSnapshot;
import com.keke.monitor.domain.port.SystemInfoCollector;
import com.keke.monitor.domain.valueobject.CpuInfo;
import com.keke.monitor.domain.valueobject.DiskInfo;
import com.keke.monitor.domain.valueobject.MemoryInfo;
import com.keke.monitor.domain.valueobject.NetworkInfo;
import com.keke.monitor.domain.valueobject.ProcessInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.Sensors;
import oshi.hardware.VirtualMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统信息采集适配器 - 使用OSHI库实现
 */
@Slf4j
@Component
public class OshiSystemInfoCollector implements SystemInfoCollector {
    
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem os;
    
    // 用于计算网络速度
    private Map<String, long[]> lastNetworkStats = new HashMap<>();
    private long lastNetworkTime = System.currentTimeMillis();
    
    public OshiSystemInfoCollector() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.os = systemInfo.getOperatingSystem();
    }
    
    @Override
    public MonitorSnapshot collectSnapshot(int topProcessCount) {
        log.debug("开始采集系统监控快照");
        
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .systemInfo(collectSystemInfo())
                .cpuInfo(collectCpuInfo())
                .memoryInfo(collectMemoryInfo())
                .diskInfoList(collectDiskInfo())
                .networkInfoList(collectNetworkInfo())
                .processInfoList(collectProcessInfo(topProcessCount))
                .build();
    }
    
    @Override
    public MonitorSnapshot collectCpuOnly() {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .cpuInfo(collectCpuInfo())
                .build();
    }
    
    @Override
    public MonitorSnapshot collectMemoryOnly() {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .memoryInfo(collectMemoryInfo())
                .build();
    }
    
    @Override
    public MonitorSnapshot collectDiskOnly() {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .diskInfoList(collectDiskInfo())
                .build();
    }
    
    @Override
    public MonitorSnapshot collectNetworkOnly() {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .networkInfoList(collectNetworkInfo())
                .build();
    }
    
    @Override
    public MonitorSnapshot collectProcessOnly(int topN) {
        return MonitorSnapshot.builder()
                .collectTime(LocalDateTime.now())
                .processInfoList(collectProcessInfo(topN))
                .build();
    }
    
    private com.keke.monitor.domain.valueobject.SystemInfo collectSystemInfo() {
        ComputerSystem computerSystem = hardware.getComputerSystem();
        Runtime runtime = Runtime.getRuntime();
        
        return com.keke.monitor.domain.valueobject.SystemInfo.builder()
                .hostname(os.getNetworkParams().getHostName())
                .osName(os.getFamily())
                .osVersion(os.getVersionInfo().getVersion())
                .osArch(System.getProperty("os.arch"))
                .bootTime(os.getSystemBootTime() * 1000)
                .upTime(os.getSystemUptime() * 1000)
                .currentUser(System.getProperty("user.name"))
                .javaVersion(System.getProperty("java.version"))
                .jvmMaxMemory(runtime.maxMemory())
                .jvmUsedMemory(runtime.totalMemory() - runtime.freeMemory())
                .jvmFreeMemory(runtime.freeMemory())
                .build();
    }
    
    private CpuInfo collectCpuInfo() {
        CentralProcessor processor = hardware.getProcessor();
        
        // 获取CPU使用率需要两次采样
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(500); // 等待500ms进行采样
        long[] currTicks = processor.getSystemCpuLoadTicks();
        
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        
        // 计算各项CPU使用率
        long user = currTicks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = currTicks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = currTicks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = currTicks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = currTicks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = currTicks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = currTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = currTicks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        
        long total = user + nice + sys + idle + iowait + irq + softirq + steal;
        
        double userPercent = total > 0 ? (user + nice) * 100.0 / total : 0;
        double systemPercent = total > 0 ? (sys + irq + softirq) * 100.0 / total : 0;
        double idlePercent = total > 0 ? idle * 100.0 / total : 0;
        
        // 获取CPU温度（可能不是所有系统都支持）
        double temperature = 0;
        Sensors sensors = hardware.getSensors();
        if (sensors.getCpuTemperature() > 0) {
            temperature = sensors.getCpuTemperature();
        }
        
        return CpuInfo.builder()
                .model(processor.getProcessorIdentifier().getName())
                .physicalCores(processor.getPhysicalProcessorCount())
                .logicalCores(processor.getLogicalProcessorCount())
                .usagePercent(cpuUsage)
                .systemPercent(systemPercent)
                .userPercent(userPercent)
                .idlePercent(idlePercent)
                .temperature(temperature)
                .frequency(processor.getMaxFreq() / 1_000_000) // 转为MHz
                .build();
    }
    
    private MemoryInfo collectMemoryInfo() {
        GlobalMemory memory = hardware.getMemory();
        
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        
        VirtualMemory virtualMemory = memory.getVirtualMemory();
        long swapTotal = virtualMemory.getSwapTotal();
        long swapUsed = virtualMemory.getSwapUsed();
        
        return MemoryInfo.builder()
                .total(total)
                .used(used)
                .available(available)
                .usagePercent(total > 0 ? used * 100.0 / total : 0)
                .swapTotal(swapTotal)
                .swapUsed(swapUsed)
                .swapUsagePercent(swapTotal > 0 ? swapUsed * 100.0 / swapTotal : 0)
                .build();
    }
    
    private List<DiskInfo> collectDiskInfo() {
        List<DiskInfo> diskInfoList = new ArrayList<>();
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        
        // 获取磁盘IO统计
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        Map<String, HWDiskStore> diskStoreMap = new HashMap<>();
        for (HWDiskStore ds : diskStores) {
            diskStoreMap.put(ds.getName(), ds);
        }
        
        for (OSFileStore fs : fileStores) {
            long total = fs.getTotalSpace();
            long usable = fs.getUsableSpace();
            long used = total - usable;
            
            DiskInfo diskInfo = DiskInfo.builder()
                    .name(fs.getMount())
                    .type(fs.getType())
                    .total(total)
                    .used(used)
                    .available(usable)
                    .usagePercent(total > 0 ? used * 100.0 / total : 0)
                    .readSpeed(0)
                    .writeSpeed(0)
                    .readCount(0)
                    .writeCount(0)
                    .build();
            
            diskInfoList.add(diskInfo);
        }
        
        return diskInfoList;
    }
    
    private List<NetworkInfo> collectNetworkInfo() {
        List<NetworkInfo> networkInfoList = new ArrayList<>();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        long currentTime = System.currentTimeMillis();
        long timeDelta = currentTime - lastNetworkTime;
        
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            
            String name = net.getName();
            long bytesRecv = net.getBytesRecv();
            long bytesSent = net.getBytesSent();
            
            // 计算网络速度
            long receiveSpeed = 0;
            long sendSpeed = 0;
            if (lastNetworkStats.containsKey(name) && timeDelta > 0) {
                long[] lastStats = lastNetworkStats.get(name);
                receiveSpeed = (bytesRecv - lastStats[0]) * 1000 / timeDelta;
                sendSpeed = (bytesSent - lastStats[1]) * 1000 / timeDelta;
            }
            lastNetworkStats.put(name, new long[]{bytesRecv, bytesSent});
            
            // 获取IP地址
            String ipv4 = "";
            String ipv6 = "";
            String[] ipv4Addrs = net.getIPv4addr();
            String[] ipv6Addrs = net.getIPv6addr();
            if (ipv4Addrs.length > 0) ipv4 = ipv4Addrs[0];
            if (ipv6Addrs.length > 0) ipv6 = ipv6Addrs[0];
            
            NetworkInfo networkInfo = NetworkInfo.builder()
                    .name(name)
                    .displayName(net.getDisplayName())
                    .macAddress(net.getMacaddr())
                    .ipv4Address(ipv4)
                    .ipv6Address(ipv6)
                    .bytesReceived(bytesRecv)
                    .bytesSent(bytesSent)
                    .receiveSpeed(Math.max(0, receiveSpeed))
                    .sendSpeed(Math.max(0, sendSpeed))
                    .packetsReceived(net.getPacketsRecv())
                    .packetsSent(net.getPacketsSent())
                    .speed(net.getSpeed())
                    .connected(net.getIfOperStatus() == NetworkIF.IfOperStatus.UP)
                    .build();
            
            networkInfoList.add(networkInfo);
        }
        
        lastNetworkTime = currentTime;
        return networkInfoList;
    }
    
    private List<ProcessInfo> collectProcessInfo(int topN) {
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, topN);
        
        return processes.stream()
                .map(this::toProcessInfo)
                .collect(Collectors.toList());
    }
    
    private ProcessInfo toProcessInfo(OSProcess process) {
        return ProcessInfo.builder()
                .pid(process.getProcessID())
                .name(process.getName())
                .path(process.getPath())
                .state(process.getState().name())
                .cpuPercent(process.getProcessCpuLoadCumulative() * 100)
                .memoryPercent(process.getResidentSetSize() * 100.0 / hardware.getMemory().getTotal())
                .virtualMemory(process.getVirtualSize())
                .residentMemory(process.getResidentSetSize())
                .threadCount(process.getThreadCount())
                .startTime(process.getStartTime())
                .upTime(process.getUpTime())
                .user(process.getUser())
                .priority(process.getPriority())
                .build();
    }
}
