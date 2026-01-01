package com.loganalyzer.ssh.interfaces.rest;

import com.loganalyzer.ssh.application.dto.DeviceInputDTO;
import com.loganalyzer.ssh.application.dto.DeviceOutputDTO;
import com.loganalyzer.ssh.application.service.DeviceApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备管理REST API
 */
@RestController
@RequestMapping("/api/ssh/devices")
@RequiredArgsConstructor
public class DeviceController {
    
    private final DeviceApplicationService deviceService;
    
    /**
     * 创建设备
     */
    @PostMapping
    public ResponseEntity<DeviceOutputDTO> createDevice(@RequestBody DeviceInputDTO input) {
        return ResponseEntity.ok(deviceService.createDevice(input));
    }
    
    /**
     * 更新设备
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeviceOutputDTO> updateDevice(
            @PathVariable Long id,
            @RequestBody DeviceInputDTO input) {
        return ResponseEntity.ok(deviceService.updateDevice(id, input));
    }
    
    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取设备详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceOutputDTO> getDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDevice(id));
    }
    
    /**
     * 获取所有设备
     */
    @GetMapping
    public ResponseEntity<List<DeviceOutputDTO>> getAllDevices(
            @RequestParam(required = false) String groupName) {
        if (groupName != null && !groupName.isEmpty()) {
            return ResponseEntity.ok(deviceService.getDevicesByGroup(groupName));
        }
        return ResponseEntity.ok(deviceService.getAllDevices());
    }
    
    /**
     * 获取所有分组名称
     */
    @GetMapping("/groups")
    public ResponseEntity<List<String>> getAllGroups() {
        return ResponseEntity.ok(deviceService.getAllGroupNames());
    }
    
    /**
     * 测试单个设备连接
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<DeviceOutputDTO> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.testConnection(id));
    }
    
    /**
     * 批量测试设备连接
     */
    @PostMapping("/batch-test")
    public ResponseEntity<List<DeviceOutputDTO>> batchTestConnection(
            @RequestBody Map<String, List<Object>> request) {
        List<Object> rawIds = request.get("deviceIds");
        List<Long> deviceIds = rawIds.stream()
                .map(id -> {
                    if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    } else {
                        return Long.parseLong(id.toString());
                    }
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(deviceService.batchTestConnection(deviceIds));
    }
    
    /**
     * 批量创建设备
     */
    @PostMapping("/batch")
    public ResponseEntity<List<DeviceOutputDTO>> batchCreateDevices(
            @RequestBody List<DeviceInputDTO> inputs) {
        List<DeviceOutputDTO> created = inputs.stream()
                .map(deviceService::createDevice)
                .collect(Collectors.toList());
        return ResponseEntity.ok(created);
    }
    
    /**
     * 导出设备列表（不含密码）
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDevices() {
        List<DeviceOutputDTO> devices = deviceService.getAllDevices();
        
        StringBuilder csv = new StringBuilder();
        // CSV头
        csv.append("设备名称,IP地址,端口,用户名,分组,状态,最后连接时间,备注\n");
        
        // CSV数据行
        for (DeviceOutputDTO device : devices) {
            csv.append(escapeCsv(device.getName())).append(",")
               .append(escapeCsv(device.getHost())).append(",")
               .append(device.getPort()).append(",")
               .append(escapeCsv(device.getUsername())).append(",")
               .append(escapeCsv(device.getGroupName())).append(",")
               .append(device.getStatus() != null ? device.getStatus() : "").append(",")
               .append(device.getLastConnectTime() != null ? device.getLastConnectTime().toString() : "").append(",")
               .append(escapeCsv(device.getRemark())).append("\n");
        }
        
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        // 添加BOM以支持Excel正确识别UTF-8
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=devices_export.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(result);
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
