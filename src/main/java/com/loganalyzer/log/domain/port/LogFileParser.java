package com.loganalyzer.log.domain.port;

import java.io.InputStream;
import java.util.List;

/**
 * 日志文件解析器端口（接口）
 * 
 * DDD概念：端口（Port）- 六边形架构
 * - 定义在领域层，由基础设施层实现
 * - 领域层通过端口与外部技术解耦
 * - 遵循依赖倒置原则：高层模块不依赖低层模块
 * 
 * 这个接口定义了"需要什么能力"，而不关心"如何实现"
 * 具体的文件读取技术细节由基础设施层的适配器实现
 */
public interface LogFileParser {

    /**
     * 解析日志文件，提取原始日志内容列表
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名（用于日志记录）
     * @return 原始日志内容列表（每条日志可能是多行的）
     */
    List<String> parseLogFile(InputStream inputStream, String filename);
    
    /**
     * 解析结果封装类
     * 包含解析的日志内容和统计信息
     */
    record ParseResult(
        List<String> logContents,   // 解析出的日志内容列表
        int totalLines,              // 文件总行数
        int logCount                 // 日志条数
    ) {}
    
    /**
     * 解析日志文件，返回详细结果
     * 
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @return 解析结果（包含统计信息）
     */
    ParseResult parseWithStats(InputStream inputStream, String filename);
}
