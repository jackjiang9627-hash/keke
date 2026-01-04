package com.keke.log.infrastructure.persistence.repository;

import com.keke.log.infrastructure.persistence.entity.LogEntryPO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository - Spring Data JPA接口
 * 
 * 这是技术实现层面的接口，用于与数据库交互
 */
@Repository
public interface LogEntryJpaRepository extends JpaRepository<LogEntryPO, String> {

    /**
     * 根据日志级别查找
     */
    List<LogEntryPO> findByLevel(String level);

    /**
     * 根据应用名称查找
     */
    List<LogEntryPO> findByApplication(String application);

    /**
     * 根据时间范围查找
     */
    List<LogEntryPO> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找未清洗的日志
     */
    List<LogEntryPO> findByCleanedFalse();

    /**
     * 查找错误级别的日志（ERROR和FATAL）
     */
    @Query("SELECT l FROM LogEntryPO l WHERE l.level IN ('ERROR', 'FATAL')")
    List<LogEntryPO> findErrors();

    /**
     * 根据级别统计数量
     */
    long countByLevel(String level);

    /**
     * 分页查询（按时间倒序）
     */
    @Query("SELECT l FROM LogEntryPO l ORDER BY l.timestamp DESC")
    List<LogEntryPO> findAllOrderByTimestampDesc(Pageable pageable);

    /**
     * 根据应用和环境查找
     */
    List<LogEntryPO> findByApplicationAndEnvironment(String application, String environment);

    /**
     * 关键词搜索（在原始内容和清洗后内容中搜索）
     */
    @Query("SELECT l FROM LogEntryPO l WHERE " +
           "LOWER(l.rawContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.cleanedContent) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<LogEntryPO> searchByKeyword(@Param("keyword") String keyword);
}
