package com.loganalyzer.log.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.log.domain.entity.LogEntry;
import com.loganalyzer.log.domain.repository.LogEntryRepository;
import com.loganalyzer.log.domain.valueobject.LogId;
import com.loganalyzer.log.domain.valueobject.LogLevel;
import com.loganalyzer.log.domain.valueobject.LogSource;
import com.loganalyzer.log.infrastructure.persistence.entity.LogEntryPO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 日志仓储实现 - 基础设施层
 * 
 * DDD概念：仓储实现（Repository Implementation）
 * - 实现领域层定义的仓储接口
 * - 负责领域对象和持久化对象之间的转换
 * - 封装数据访问技术细节
 */
@Repository
public class LogEntryRepositoryImpl implements LogEntryRepository {

    private final LogEntryJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public LogEntryRepositoryImpl(LogEntryJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public LogEntry save(LogEntry logEntry) {
        LogEntryPO po = toPO(logEntry);
        LogEntryPO saved = jpaRepository.save(po);
        return toDomain(saved);
    }

    @Override
    public List<LogEntry> saveAll(List<LogEntry> logEntries) {
        List<LogEntryPO> poList = logEntries.stream().map(this::toPO).toList();
        List<LogEntryPO> savedList = jpaRepository.saveAll(poList);
        return savedList.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<LogEntry> findById(LogId id) {
        return jpaRepository.findById(id.getValue()).map(this::toDomain);
    }

    @Override
    public void deleteById(LogId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public List<LogEntry> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaRepository.findAllOrderByTimestampDesc(pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findByLevel(LogLevel level) {
        return jpaRepository.findByLevel(level.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findBySource(LogSource source) {
        return jpaRepository.findByApplicationAndEnvironment(
                source.getApplication(), source.getEnvironment())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findByApplication(String application) {
        return jpaRepository.findByApplication(application)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByTimestampBetween(start, end)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findUncleaned() {
        return jpaRepository.findByCleanedFalse()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LogEntry> findErrors() {
        return jpaRepository.findErrors()
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByLevel(LogLevel level) {
        return jpaRepository.countByLevel(level.name());
    }

    // ========== 领域对象与持久化对象的转换 ==========

    /**
     * 领域对象 -> 持久化对象
     */
    private LogEntryPO toPO(LogEntry logEntry) {
        LogEntryPO po = new LogEntryPO();
        po.setId(logEntry.getId().getValue());
        po.setRawContent(logEntry.getRawContent());
        po.setCleanedContent(logEntry.getCleanedContent());
        po.setLevel(logEntry.getLevel().name());
        
        if (logEntry.getSource() != null) {
            po.setApplication(logEntry.getSource().getApplication());
            po.setHost(logEntry.getSource().getHost());
            po.setEnvironment(logEntry.getSource().getEnvironment());
        }
        
        po.setTimestamp(logEntry.getTimestamp());
        po.setCreatedAt(logEntry.getCreatedAt());
        po.setCleaned(logEntry.isCleaned());
        po.setMetadata(serializeMetadata(logEntry.getMetadata()));
        
        return po;
    }

    /**
     * 持久化对象 -> 领域对象
     */
    private LogEntry toDomain(LogEntryPO po) {
        LogSource source = new LogSource(
                po.getApplication(),
                po.getHost(),
                po.getEnvironment()
        );
        
        return LogEntry.reconstitute(
                po.getId(),
                po.getRawContent(),
                po.getCleanedContent(),
                LogLevel.fromString(po.getLevel()),
                source,
                po.getTimestamp(),
                po.getCreatedAt(),
                po.isCleaned(),
                deserializeMetadata(po.getMetadata())
        );
    }

    /**
     * 序列化元数据为JSON
     */
    private String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 反序列化JSON为元数据
     */
    private Map<String, String> deserializeMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}
