package com.loganalyzer.assistant.infrastructure.persistence.repository;

import com.loganalyzer.assistant.infrastructure.persistence.entity.CaseEntryPO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 案例JPA仓储接口
 */
public interface CaseEntryJpaRepository extends JpaRepository<CaseEntryPO, String> {
    
    Page<CaseEntryPO> findByModuleName(String moduleName, Pageable pageable);
    
    long countByModuleName(String moduleName);
    
    @Query("SELECT c FROM CaseEntryPO c WHERE c.title LIKE %:keyword% OR c.summary LIKE %:keyword%")
    List<CaseEntryPO> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT DISTINCT c.moduleName FROM CaseEntryPO c ORDER BY c.moduleName")
    List<String> findAllModuleNames();
    
    /**
     * 根据标题和模块名称查找（用于去重检查）
     * 返回第一条匹配记录
     */
    Optional<CaseEntryPO> findFirstByTitleAndModuleName(String title, String moduleName);
    
    /**
     * 查找今日待复习的案例（启用复习且下次复习日期<=今天）
     */
    List<CaseEntryPO> findByReviewEnabledTrueAndNextReviewDateLessThanEqual(LocalDate date);
}
