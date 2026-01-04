package com.keke.log.application.service;

import com.keke.log.application.assembler.LogAssembler;
import com.keke.log.application.dto.LogInputDTO;
import com.keke.log.application.dto.LogOutputDTO;
import com.keke.log.application.dto.LogStatisticsDTO;
import com.keke.log.domain.entity.LogEntry;
import com.keke.log.domain.port.LogFileParser;
import com.keke.log.domain.repository.LogEntryRepository;
import com.keke.log.domain.service.LogAnalysisService;
import com.keke.log.domain.service.LogCleansingService;
import com.keke.log.domain.valueobject.LogId;
import com.keke.log.domain.valueobject.LogLevel;
import com.keke.log.domain.valueobject.LogSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志应用服务
 * 
 * DDD概念：应用服务（Application Service）
 * - 编排领域对象和领域服务
 * - 处理用例流程
 * - 事务边界
 * - 不包含业务逻辑（业务逻辑在领域层）
 * 
 * 职责：
 * 1. 接收DTO，调用领域服务
 * 2. 协调多个聚合
 * 3. 发布领域事件
 * 4. 返回DTO给接口层
 */
@Slf4j
@Service
@Transactional
public class LogApplicationService {

    private final LogEntryRepository logEntryRepository;
    private final LogCleansingService logCleansingService;
    private final LogAnalysisService logAnalysisService;
    private final LogFileParser logFileParser;  // 端口接口，依赖倒置

    public LogApplicationService(LogEntryRepository logEntryRepository,
                                 LogCleansingService logCleansingService,
                                 LogAnalysisService logAnalysisService,
                                 LogFileParser logFileParser) {
        this.logEntryRepository = logEntryRepository;
        this.logCleansingService = logCleansingService;
        this.logAnalysisService = logAnalysisService;
        this.logFileParser = logFileParser;
    }

    /**
     * 接收并处理单条日志
     * 
     * 用例流程：
     * 1. 创建日志实体
     * 2. 执行清洗
     * 3. 持久化
     * 4. 返回结果
     */
    public LogOutputDTO receiveAndProcess(LogInputDTO input) {
        log.info("接收日志: application={}, environment={}", input.getApplication(), input.getEnvironment());
        
        // 1. 创建日志来源值对象
        LogSource source = new LogSource(
                input.getApplication(),
                input.getHost(),
                input.getEnvironment()
        );

        // 2. 创建日志实体（聚合根）
        LogEntry logEntry = LogEntry.createRaw(input.getContent(), source);
        log.debug("创建日志实体: id={}", logEntry.getId());

        // 3. 执行清洗（调用领域服务）
        logCleansingService.cleanse(logEntry);
        log.debug("日志清洗完成: level={}, cleaned={}", logEntry.getLevel(), logEntry.isCleaned());

        // 4. 持久化（调用仓储）
        LogEntry saved = logEntryRepository.save(logEntry);
        log.info("日志保存成功: id={}, level={}", saved.getId(), saved.getLevel());

        // 5. 转换为DTO返回
        return LogAssembler.toOutputDTO(saved);
    }

    /**
     * 批量接收并处理日志
     */
    public List<LogOutputDTO> batchReceiveAndProcess(List<LogInputDTO> inputs) {
        log.info("批量接收日志: count={}", inputs.size());
        
        List<LogEntry> logEntries = inputs.stream()
                .map(input -> {
                    LogSource source = new LogSource(
                            input.getApplication(),
                            input.getHost(),
                            input.getEnvironment()
                    );
                    return LogEntry.createRaw(input.getContent(), source);
                })
                .toList();

        // 批量清洗
        logCleansingService.cleanseAll(logEntries);
        log.debug("批量清洗完成: count={}", logEntries.size());

        // 批量保存
        List<LogEntry> saved = logEntryRepository.saveAll(logEntries);
        log.info("批量保存成功: count={}", saved.size());

        return LogAssembler.toOutputDTOList(saved);
    }

    /**
     * 根据ID查询日志
     */
    @Transactional(readOnly = true)
    public LogOutputDTO findById(String id) {
        return logEntryRepository.findById(LogId.of(id))
                .map(LogAssembler::toOutputDTO)
                .orElse(null);
    }

    /**
     * 查询所有日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findAll() {
        List<LogEntry> entries = logEntryRepository.findAll();
        return LogAssembler.toOutputDTOList(entries);
    }

    /**
     * 分页查询日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findAll(int page, int size) {
        List<LogEntry> entries = logEntryRepository.findAll(page, size);
        return LogAssembler.toOutputDTOList(entries);
    }
    
    /**
     * 统计日志总数
     */
    @Transactional(readOnly = true)
    public long count() {
        return logEntryRepository.count();
    }

    /**
     * 根据级别查询日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findByLevel(String level) {
        LogLevel logLevel = LogLevel.fromString(level);
        List<LogEntry> entries = logEntryRepository.findByLevel(logLevel);
        return LogAssembler.toOutputDTOList(entries);
    }

    /**
     * 根据应用名称查询日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findByApplication(String application) {
        List<LogEntry> entries = logEntryRepository.findByApplication(application);
        return LogAssembler.toOutputDTOList(entries);
    }

    /**
     * 根据时间范围查询日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        List<LogEntry> entries = logEntryRepository.findByTimestampBetween(start, end);
        return LogAssembler.toOutputDTOList(entries);
    }

    /**
     * 查询错误日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> findErrors() {
        List<LogEntry> entries = logEntryRepository.findErrors();
        return LogAssembler.toOutputDTOList(entries);
    }

    /**
     * 获取日志统计信息
     */
    @Transactional(readOnly = true)
    public LogStatisticsDTO getStatistics() {
        List<LogEntry> allEntries = logEntryRepository.findAll();
        LogAnalysisService.LogStatistics statistics = logAnalysisService.analyze(allEntries);
        return LogAssembler.toStatisticsDTO(statistics);
    }

    /**
     * 关键词搜索日志
     */
    @Transactional(readOnly = true)
    public List<LogOutputDTO> searchByKeyword(String keyword) {
        List<LogEntry> allEntries = logEntryRepository.findAll();
        List<LogEntry> filtered = logAnalysisService.searchByKeyword(allEntries, keyword);
        return LogAssembler.toOutputDTOList(filtered);
    }

    /**
     * 删除日志
     */
    public void deleteById(String id) {
        log.info("删除日志: id={}", id);
        logEntryRepository.deleteById(LogId.of(id));
    }

    /**
     * 重新清洗未清洗的日志
     */
    public List<LogOutputDTO> reprocessUncleaned() {
        List<LogEntry> uncleaned = logEntryRepository.findUncleaned();
        logCleansingService.cleanseAll(uncleaned);
        List<LogEntry> saved = logEntryRepository.saveAll(uncleaned);
        return LogAssembler.toOutputDTOList(saved);
    }

    /**
     * 上传日志文件并清洗
     * 
     * DDD概念：应用服务只做编排
     * - 调用端口（LogFileParser）解析文件
     * - 调用领域服务清洗日志
     * - 调用仓储保存日志
     * - 不包含任何技术实现细节
     * 
     * @param file 上传的日志文件
     * @param application 应用名称
     * @param environment 环境
     * @return 处理结果列表
     */
    public List<LogOutputDTO> uploadAndProcessFile(MultipartFile file, String application, String environment) {
        log.info("上传日志文件: filename={}, size={}, application={}", 
                file.getOriginalFilename(), file.getSize(), application);
        
        // 1. 调用端口解析文件（技术细节在基础设施层）
        LogFileParser.ParseResult parseResult;
        try {
            parseResult = logFileParser.parseWithStats(
                    file.getInputStream(), 
                    file.getOriginalFilename()
            );
        } catch (IOException e) {
            log.error("获取文件流失败: {}", e.getMessage());
            throw new RuntimeException("获取文件流失败: " + e.getMessage(), e);
        }
        
        log.info("文件解析完成: 总行数={}, 日志条数={}", 
                parseResult.totalLines(), parseResult.logCount());
        
        // 2. 将原始内容转换为领域实体
        List<LogEntry> logEntries = parseResult.logContents().stream()
                .map(content -> createLogEntry(content, application, environment))
                .toList();
        
        // 3. 调用领域服务批量清洗
        logCleansingService.cleanseAll(logEntries);
        log.debug("文件日志清洗完成");
        
        // 4. 调用仓储批量保存
        List<LogEntry> saved = logEntryRepository.saveAll(logEntries);
        log.info("文件日志保存成功: count={}", saved.size());
        
        // 5. 转换为DTO返回
        return LogAssembler.toOutputDTOList(saved);
    }
    
    /**
     * 创建日志实体（工厂方法）
     */
    private LogEntry createLogEntry(String content, String application, String environment) {
        LogSource source = new LogSource(
                application != null ? application : "uploaded-file",
                "file-upload",
                environment != null ? environment : "unknown"
        );
        return LogEntry.createRaw(content, source);
    }
}
