package com.loganalyzer.assistant.domain.entity;

import com.loganalyzer.assistant.domain.valueobject.CaseId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 案例实体 - 聚合根
 * 
 * DDD概念：聚合根（Aggregate Root）
 * - 案例库的核心实体
 * - 包含标题、摘要、超链接、内容等信息
 * - 按模块分类（支持自定义模块）
 */
@Getter
public class CaseEntry {
    
    private CaseId id;
    private String title;           // 标题
    private String summary;         // 摘要
    private String hyperlink;       // 超链接（可选）
    private String content;         // 内容文本（可选）
    private String moduleName;      // 所属模块（自定义字符串）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 用于语义搜索的向量（由基础设施层计算）
    private float[] embedding;
    
    private CaseEntry() {}
    
    /**
     * 创建新案例
     */
    public static CaseEntry create(String title, String summary, String hyperlink, 
                                   String content, String moduleName) {
        CaseEntry entry = new CaseEntry();
        entry.id = CaseId.generate();
        entry.title = title;
        entry.summary = summary;
        entry.hyperlink = hyperlink;
        entry.content = content;
        entry.moduleName = moduleName != null ? moduleName.trim() : "默认";
        entry.createdAt = LocalDateTime.now();
        entry.updatedAt = LocalDateTime.now();
        return entry;
    }
    
    /**
     * 重建案例（从持久化恢复）
     */
    public static CaseEntry reconstitute(CaseId id, String title, String summary,
                                         String hyperlink, String content, 
                                         String moduleName,
                                         LocalDateTime createdAt, LocalDateTime updatedAt,
                                         float[] embedding) {
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
        return entry;
    }
    
    /**
     * 更新案例信息
     */
    public void update(String title, String summary, String hyperlink, 
                       String content, String moduleName) {
        this.title = title;
        this.summary = summary;
        this.hyperlink = hyperlink;
        this.content = content;
        this.moduleName = moduleName != null ? moduleName.trim() : this.moduleName;
        this.updatedAt = LocalDateTime.now();
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
}
