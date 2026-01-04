package com.keke.monitor.domain.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络信息值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInfo {
    
    /** 网卡名称 */
    private String name;
    
    /** 网卡显示名称 */
    private String displayName;
    
    /** MAC地址 */
    private String macAddress;
    
    /** IPv4地址 */
    private String ipv4Address;
    
    /** IPv6地址 */
    private String ipv6Address;
    
    /** 接收字节数 */
    private long bytesReceived;
    
    /** 发送字节数 */
    private long bytesSent;
    
    /** 接收速度(bytes/s) */
    private long receiveSpeed;
    
    /** 发送速度(bytes/s) */
    private long sendSpeed;
    
    /** 接收包数 */
    private long packetsReceived;
    
    /** 发送包数 */
    private long packetsSent;
    
    /** 网络带宽(bps) */
    private long speed;
    
    /** 是否已连接 */
    private boolean connected;
    
    public String formatSpeed() {
        return formatBps(speed);
    }
    
    public String formatReceiveSpeed() {
        return formatBytes(receiveSpeed) + "/s";
    }
    
    public String formatSendSpeed() {
        return formatBytes(sendSpeed) + "/s";
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String formatBps(long bps) {
        if (bps < 1000) return bps + " bps";
        if (bps < 1000000) return String.format("%.2f Kbps", bps / 1000.0);
        if (bps < 1000000000) return String.format("%.2f Mbps", bps / 1000000.0);
        return String.format("%.2f Gbps", bps / 1000000000.0);
    }
}
