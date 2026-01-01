package com.loganalyzer.assistant.domain.entity;

import com.loganalyzer.assistant.domain.valueobject.CaseId;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案例实体 - 聚合根
 * 
 * DDD概念：聚合根（Aggregate Root）
 * - 案例库的核心实体
 * - 包含标题、摘要、超链接、内容等信息
 * - 按模块分类（支持自定义模块）
 * - 支持AI对话总结和遗忘曲线复习
 */
@Getter
public class CaseEntry {
    
    private CaseId id;
    private String title;           // 标题
    private String summary;         // 摘要
    private String hyperlink;       // 超链接（可选）
    private String content;         // 内容文本（可选，支持Markdown）
    private String moduleName;      // 所属模块（自定义字符串）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 用于语义搜索的向量（由基础设施层计算）
    private float[] embedding;
    
    // === AI总结相关字段 ===
    private String tags;                    // 标签（逗号分隔）
    private String source;                  // 来源：MANUAL-手动添加, AI_CHAT-AI对话总结
    private String originalConversation;    // 原始对话记录(JSON格式)
    
    // === 遗忘曲线复习相关字段 ===
    private LocalDate nextReviewDate;       // 下次复习日期
    private Integer reviewCount;            // 复习次数
    private Integer masteryLevel;           // 熟练度 1-5
    private LocalDateTime lastReviewTime;   // 上次复习时间
    private Boolean reviewEnabled;          // 是否启用复习
    
    // 复习间隔天数（艾宾浩斯遗忘曲线）
    private static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30, 60};
    
    private CaseEntry() {}
    
    /**
     * 创建新案例（手动添加）
     */
    public static CaseEntry create(String title, String summary, String hyperlink, 
                                   String content, String moduleName) {
        return create(title, summary, hyperlink, content, moduleName, null, "MANUAL", null);
    }
    
    /**
     * 创建新案例（完整参数）
     */
    public static CaseEntry create(String title, String summary, String hyperlink, 
                                   String content, String moduleName, String tags,
                                   String source, String originalConversation) {
        CaseEntry entry = new CaseEntry();
        entry.id = CaseId.generate();
        entry.title = title;
        entry.summary = summary;
        entry.hyperlink = hyperlink;
        entry.content = content;
        entry.moduleName = moduleName != null ? moduleName.trim() : "默认";
        entry.tags = tags;
        entry.source = source != null ? source : "MANUAL";
        entry.originalConversation = originalConversation;
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = LocalDateTime.now();
        // 默认启用复习
        entry.reviewEnabled = true;
        entry.reviewCount = 0;
        entry.masteryLevel = 1;
        entry.nextReviewDate = LocalDate.now().plusDays(1); // 第一次复习在明天
        return entry;
    }
    
    /**
     * 重建案例（从持久化恢复）
     */
    public static CaseEntry reconstitute(CaseId id, String title, String summary,
                                         String hyperlink, String content, 
                                         String moduleName,
                                         LocalDateTime createdAt, LocalDateTime updatedAt,
                                         float[] embedding,
                                         String tags, String source, String originalConversation,
                                         LocalDate nextReviewDate, Integer reviewCount,
                                         Integer masteryLevel, LocalDateTime lastReviewTime,
                                         Boolean reviewEnabled) {
        CaseEntry entry = new CaseEntry();
        entry.id = id;
        entry.title = title;
        entry.summary = summary;
        entry.hyperlink = hyperlink;
        entry.content = content;
        entry.moduleName = moduleName;
        entry.createdAt = createdAt;
        entry.updatedAt = updatedAt;
        entry.embedding = embedding;
        entry.tags = tags;
        entry.source = source;
        entry.originalConversation = originalConversation;
        entry.nextReviewDate = nextReviewDate;
        entry.reviewCount = reviewCount != null ? reviewCount : 0;
        entry.masteryLevel = masteryLevel != null ? masteryLevel : 1;
        entry.lastReviewTime = lastReviewTime;
        entry.reviewEnabled = reviewEnabled != null ? reviewEnabled : true;
        return entry;
    }
    
    /**
     * 更新案例信息
     */
    public void update(String title, String summary, String hyperlink, 
                       String content, String moduleName) {
        update(title, summary, hyperlink, content, moduleName, this.tags);
    }
    
    /**
     * 更新案例信息（含标签）
     */
    public void update(String title, String summary, String hyperlink, 
                       String content, String moduleName, String tags) {
        this.title = title;
        this.summary = summary;
        this.hyperlink = hyperlink;
        this.content = content;
        this.moduleName = moduleName != null ? moduleName.trim() : this.moduleName;
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置复习启用状态
     */
    public void setReviewEnabled(Boolean enabled) {
        this.reviewEnabled = enabled;
    }
    
    /**
     * 标记已复习（用户表示已掌握）
     */
    public void markAsReviewed(boolean mastered) {
        this.lastReviewTime = LocalDateTime.now();
        this.reviewCount = (this.reviewCount != null ? this.reviewCount : 0) + 1;
        
        if (mastered) {
            // 已掌握，提升熟练度，延长复习间隔
            this.masteryLevel = Math.min(5, (this.masteryLevel != null ? this.masteryLevel : 1) + 1);
            int intervalIndex = Math.min(this.reviewCount - 1, REVIEW_INTERVALS.length - 1);
            this.nextReviewDate = LocalDate.now().plusDays(REVIEW_INTERVALS[intervalIndex]);
        } else {
            // 未掌握，保持熟练度，明天再复习
            this.nextReviewDate = LocalDate.now().plusDays(1);
        }
    }
    
    /**
     * 延后复习到明天
     */
    public void postponeReview() {
        this.nextReviewDate = LocalDate.now().plusDays(1);
    }
    
    /**
     * 是否需要今天复习
     */
    public boolean needsReviewToday() {
        if (this.reviewEnabled == null || !this.reviewEnabled) {
            return false;
        }
        if (this.nextReviewDate == null) {
            return false;
        }
        return !this.nextReviewDate.isAfter(LocalDate.now());
    }
    
    /**
     * 设置嵌入向量（用于语义搜索）
     */
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
    
    /**
     * 获取用于搜索的文本内容
     */
    public String getSearchableText() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" ");
        if (summary != null) sb.append(summary).append(" ");
        if (content != null) sb.append(content);
        return sb.toString();
    }
    
    /**
     * 获取截断的摘要（用于列表展示）
     */
    public String getTruncatedSummary(int maxLength) {
        if (summary == null) return "";
        if (summary.length() <= maxLength) return summary;
        return summary.substring(0, maxLength) + "...";
    }
    
    /**
     * 是否有超链接
     */
    public boolean hasHyperlink() {
        return hyperlink != null && !hyperlink.isBlank();
    }
    
    /**
     * 是否来自AI对话
     */
    public boolean isFromAiChat() {
        return "AI_CHAT".equals(source);
    }
}
