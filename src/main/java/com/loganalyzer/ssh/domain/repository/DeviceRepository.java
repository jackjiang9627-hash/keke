package com.loganalyzer.ssh.domain.repository;

import com.loganalyzer.ssh.domain.entity.Device;

import java.util.List;
import java.util.Optional;

/**
 * 设备仓储接口
 */
public interface DeviceRepository {
    
    Device save(Device device);
    
    Optional<Device> findById(Long id);
    
    List<Device> findAll();
    
    List<Device> findByGroupName(String groupName);
    
    List<Device> findByIds(List<Long> ids);
    
    void deleteById(Long id);
    
    boolean existsByHostAndPort(String host, Integer port);
    
    List<String> findAllGroupNames();
}
