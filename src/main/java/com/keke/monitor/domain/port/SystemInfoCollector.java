package com.keke.monitor.domain.port;

import com.keke.monitor.domain.entity.MonitorSnapshot;

/**
 * 系统信息采集端口 - 六边形架构的端口接口
 */
public interface SystemInfoCollector {
    
    /**
     * 采集完整的系统监控快照
     * @param topProcessCount 返回的进程数量
     * @return 系统监控快照
     */
    MonitorSnapshot collectSnapshot(int topProcessCount);
    
    /**
     * 采集CPU信息
     */
    MonitorSnapshot collectCpuOnly();
    
    /**
     * 采集内存信息
     */
    MonitorSnapshot collectMemoryOnly();
    
    /**
     * 采集磁盘信息
     */
    MonitorSnapshot collectDiskOnly();
    
    /**
     * 采集网络信息
     */
    MonitorSnapshot collectNetworkOnly();
    
    /**
     * 采集进程信息
     * @param topN 返回的进程数量
     */
    MonitorSnapshot collectProcessOnly(int topN);
}
