package com.keke.ssh.application.service;

import com.keke.ssh.application.dto.DeviceInputDTO;
import com.keke.ssh.application.dto.DeviceOutputDTO;
import com.keke.ssh.domain.entity.Device;
import com.keke.ssh.domain.port.SshExecutor;
import com.keke.ssh.domain.repository.DeviceRepository;
import com.keke.ssh.domain.valueobject.AuthType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApplicationService {
    
    private final DeviceRepository deviceRepository;
    private final SshExecutor sshExecutor;
    
    /**
     * 创建设备
     */
    @Transactional
    public DeviceOutputDTO createDevice(DeviceInputDTO input) {
        Device device = Device.create(
                input.getName(),
                input.getHost(),
                input.getPort() != null ? input.getPort() : 22,
                input.getUsername()
        );
        device.setPassword(input.getPassword());
        device.setPrivateKey(input.getPrivateKey());
        device.setAuthType(input.getAuthType() != null ? AuthType.valueOf(input.getAuthType()) : AuthType.PASSWORD);
        device.setGroupName(input.getGroupName());
        device.setRemark(input.getRemark());
        
        Device saved = deviceRepository.save(device);
        log.info("创建设备成功: {} ({}:{})", saved.getName(), saved.getHost(), saved.getPort());
        return toOutputDTO(saved);
    }
    
    /**
     * 更新设备
     */
    @Transactional
    public DeviceOutputDTO updateDevice(Long id, DeviceInputDTO input) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + id));
        
        device.setName(input.getName());
        device.setHost(input.getHost());
        device.setPort(input.getPort() != null ? input.getPort() : 22);
        device.setUsername(input.getUsername());
        if (input.getPassword() != null) {
            device.setPassword(input.getPassword());
        }
        if (input.getPrivateKey() != null) {
            device.setPrivateKey(input.getPrivateKey());
        }
        device.setAuthType(input.getAuthType() != null ? AuthType.valueOf(input.getAuthType()) : AuthType.PASSWORD);
        device.setGroupName(input.getGroupName());
        device.setRemark(input.getRemark());
        device.setUpdateTime(LocalDateTime.now());
        
        Device saved = deviceRepository.save(device);
        log.info("更新设备成功: {} ({}:{})", saved.getName(), saved.getHost(), saved.getPort());
        return toOutputDTO(saved);
    }
    
    /**
     * 删除设备
     */
    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
        log.info("删除设备成功: {}", id);
    }
    
    /**
     * 获取设备详情
     */
    public DeviceOutputDTO getDevice(Long id) {
        return deviceRepository.findById(id)
                .map(this::toOutputDTO)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + id));
    }
    
    /**
     * 获取所有设备
     */
    public List<DeviceOutputDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::toOutputDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 按分组获取设备
     */
    public List<DeviceOutputDTO> getDevicesByGroup(String groupName) {
        return deviceRepository.findByGroupName(groupName).stream()
                .map(this::toOutputDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有分组名称
     */
    public List<String> getAllGroupNames() {
        return deviceRepository.findAllGroupNames();
    }
    
    /**
     * 测试设备连接
     */
    @Transactional
    public DeviceOutputDTO testConnection(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + id));
        
        String error = sshExecutor.testConnection(device);
        device.updateConnectStatus(error == null, error == null ? "连接成功" : error);
        
        Device saved = deviceRepository.save(device);
        log.info("连接测试完成: {} - {}", saved.getName(), saved.getLastConnectResult());
        return toOutputDTO(saved);
    }
    
    /**
     * 批量测试设备连接
     */
    @Transactional
    public List<DeviceOutputDTO> batchTestConnection(List<Long> deviceIds) {
        List<Device> devices = deviceRepository.findByIds(deviceIds);
        
        for (Device device : devices) {
            String error = sshExecutor.testConnection(device);
            device.updateConnectStatus(error == null, error == null ? "连接成功" : error);
            deviceRepository.save(device);
        }
        
        return devices.stream()
                .map(this::toOutputDTO)
                .collect(Collectors.toList());
    }
    
    private DeviceOutputDTO toOutputDTO(Device device) {
        return DeviceOutputDTO.builder()
                .id(device.getId())
                .name(device.getName())
                .host(device.getHost())
                .port(device.getPort())
                .username(device.getUsername())
                .authType(device.getAuthType() != null ? device.getAuthType().name() : null)
                .groupName(device.getGroupName())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .lastConnectTime(device.getLastConnectTime())
                .lastConnectResult(device.getLastConnectResult())
                .remark(device.getRemark())
                .createTime(device.getCreateTime())
                .build();
    }
}
