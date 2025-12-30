package com.loganalyzer.log.infrastructure.adapter;

import com.loganalyzer.log.domain.port.LogFileParser;
import com.loganalyzer.log.domain.service.LogLineDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志文件解析器适配器
 * 
 * DDD概念：适配器（Adapter）- 六边形架构
 * - 实现领域层定义的端口接口
 * - 包含具体的技术实现细节（文件IO）
 * - 将外部技术转换为领域层可以理解的形式
 * 
 * 职责：
 * 1. 读取文件输入流
 * 2. 处理多行日志（如堆栈跟踪）
 * 3. 使用领域服务判断日志边界
 * 4. 返回解析后的日志内容列表
 */
@Slf4j
@Component
public class LogFileParserImpl implements LogFileParser {

    private final LogLineDetector logLineDetector;

    public LogFileParserImpl(LogLineDetector logLineDetector) {
        this.logLineDetector = logLineDetector;
    }

    @Override
    public List<String> parseLogFile(InputStream inputStream, String filename) {
        return parseWithStats(inputStream, filename).logContents();
    }

    @Override
    public ParseResult parseWithStats(InputStream inputStream, String filename) {
        log.debug("开始解析日志文件: {}", filename);
        
        List<String> logContents = new ArrayList<>();
        int lineCount = 0;
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            StringBuilder currentLog = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 使用领域服务判断是否是新日志行
                if (logLineDetector.isNewLogLine(line) && currentLog.length() > 0) {
                    // 保存之前累积的日志
                    logContents.add(currentLog.toString());
                    currentLog = new StringBuilder();
                }
                
                // 累积当前行
                if (currentLog.length() > 0) {
                    currentLog.append("\n");
                }
                currentLog.append(line);
            }
            
            // 处理最后一条日志
            if (currentLog.length() > 0) {
                logContents.add(currentLog.toString());
            }
            
        } catch (IOException e) {
            log.error("读取日志文件失败: filename={}, error={}", filename, e.getMessage());
            throw new LogFileParseException("读取日志文件失败: " + e.getMessage(), e);
        }
        
        log.info("日志文件解析完成: filename={}, totalLines={}, logCount={}", 
                filename, lineCount, logContents.size());
        
        return new ParseResult(logContents, lineCount, logContents.size());
    }
    
    /**
     * 日志文件解析异常
     * 将技术异常转换为业务异常
     */
    public static class LogFileParseException extends RuntimeException {
        public LogFileParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
