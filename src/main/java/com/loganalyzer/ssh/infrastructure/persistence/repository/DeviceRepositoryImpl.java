package com.loganalyzer.ssh.infrastructure.persistence.repository;

import com.loganalyzer.ssh.domain.entity.Device;
import com.loganalyzer.ssh.domain.repository.DeviceRepository;
import com.loganalyzer.ssh.domain.valueobject.AuthType;
import com.loganalyzer.ssh.domain.valueobject.DeviceStatus;
import com.loganalyzer.ssh.infrastructure.persistence.entity.DeviceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {
    
    private final DeviceJpaRepository jpaRepository;
    
    @Override
    public Device save(Device device) {
        DeviceEntity entity = toEntity(device);
        DeviceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<Device> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public List<Device> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Device> findByGroupName(String groupName) {
        return jpaRepository.findByGroupName(groupName).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Device> findByIds(List<Long> ids) {
        return jpaRepository.findByIdIn(ids).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByHostAndPort(String host, Integer port) {
        return jpaRepository.existsByHostAndPort(host, port);
    }
    
    @Override
    public List<String> findAllGroupNames() {
        return jpaRepository.findAllGroupNames();
    }
    
    private DeviceEntity toEntity(Device device) {
        return DeviceEntity.builder()
                .id(device.getId())
                .name(device.getName())
                .host(device.getHost())
                .port(device.getPort())
                .username(device.getUsername())
                .password(device.getPassword())
                .privateKey(device.getPrivateKey())
                .authType(device.getAuthType() != null ? device.getAuthType().name() : null)
                .groupName(device.getGroupName())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .lastConnectTime(device.getLastConnectTime())
                .lastConnectResult(device.getLastConnectResult())
                .remark(device.getRemark())
                .createTime(device.getCreateTime())
                .updateTime(device.getUpdateTime())
                .build();
    }
    
    private Device toDomain(DeviceEntity entity) {
        return Device.builder()
                .id(entity.getId())
                .name(entity.getName())
                .host(entity.getHost())
                .port(entity.getPort())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .privateKey(entity.getPrivateKey())
                .authType(entity.getAuthType() != null ? AuthType.valueOf(entity.getAuthType()) : null)
                .groupName(entity.getGroupName())
                .status(entity.getStatus() != null ? DeviceStatus.valueOf(entity.getStatus()) : null)
                .lastConnectTime(entity.getLastConnectTime())
                .lastConnectResult(entity.getLastConnectResult())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
