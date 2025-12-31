# 日志分析与工作助手系统 - DDD架构设计文档

## 1. 系统概述

本系统是一个基于领域驱动设计(DDD)的智能日志分析和工作助手平台，包含三个独立的限界上下文：
- **日志分析（Log Context）**：日志接收、清洗、分析、存储
- **工作助手（Assistant Context）**：案例库管理、Todo管理、智能问答
- **系统监控（Monitor Context）**：系统指标采集、进程监控、历史数据查询

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                      接口层 (Interfaces)                      │
│  REST API Controllers, 请求/响应处理                          │
├─────────────────────────────────────────────────────────────┤
│                      应用层 (Application)                     │
│  应用服务(用例编排), DTO, Assembler                           │
├─────────────────────────────────────────────────────────────┤
│                      领域层 (Domain)                          │
│  实体(聚合根), 值对象, 领域服务, 仓储接口, 端口接口             │
├─────────────────────────────────────────────────────────────┤
│                   基础设施层 (Infrastructure)                  │
│  仓储实现, 外部适配器, 持久化, 配置                            │
└─────────────────────────────────────────────────────────────┘
```

## 3. 包结构设计

按照DDD的限界上下文(Bounded Context)原则，将系统划分为四个模块：

```
com.loganalyzer/
├── LogAnalyzerApplication.java          # Spring Boot启动类
│
├── shared/                              # 共享内核（公共代码）
│   ├── application/
│   │   └── dto/
│   │       └── PageDTO.java            # 分页响应DTO
│   └── infrastructure/
│       ├── config/
│       │   └── WebConfig.java          # Web配置（CORS等）
│       ├── excel/
│       │   └── ExcelBuilder.java       # 通用Excel导出工具
│       └── python/                     # Python集成基础设施
│           ├── PythonTask.java
│           ├── PythonTaskQueue.java
│           ├── PythonProcessManager.java
│           ├── PythonGatewayServer.java
│           └── PythonBridge.java
│
├── log/                                 # 日志分析限界上下文
│   ├── domain/                          # 领域层
│   │   ├── entity/
│   │   │   └── LogEntry.java           # 聚合根
│   │   ├── valueobject/
│   │   │   ├── LogId.java
│   │   │   ├── LogLevel.java
│   │   │   ├── LogSource.java
│   │   │   └── CleansingResult.java
│   │   ├── service/
│   │   │   ├── LogAnalysisService.java
│   │   │   ├── LogCleansingService.java
│   │   │   ├── LogCleansingStrategy.java
│   │   │   └── LogLineDetector.java
│   │   ├── port/
│   │   │   └── LogFileParser.java      # 端口接口
│   │   └── repository/
│   │       └── LogEntryRepository.java
│   │
│   ├── application/                     # 应用层
│   │   ├── service/
│   │   │   └── LogApplicationService.java
│   │   ├── dto/
│   │   │   ├── LogInputDTO.java
│   │   │   ├── LogOutputDTO.java
│   │   │   └── LogStatisticsDTO.java
│   │   └── assembler/
│   │       └── LogAssembler.java
│   │
│   ├── infrastructure/                  # 基础设施层
│   │   ├── adapter/
│   │   │   └── LogFileParserImpl.java  # 端口适配器
│   │   ├── cleansing/
│   │   │   ├── StandardLogCleansingStrategy.java
│   │   │   ├── JsonLogCleansingStrategy.java
│   │   │   └── SimpleTextCleansingStrategy.java
│   │   ├── config/
│   │   │   └── LogDomainServiceConfig.java
│   │   └── persistence/
│   │       ├── entity/
│   │       │   └── LogEntryPO.java
│   │       └── repository/
│   │           ├── LogEntryJpaRepository.java
│   │           └── LogEntryRepositoryImpl.java
│   │
│   └── interfaces/                      # 接口层
│       └── rest/
│           └── LogController.java
│
├── assistant/                           # 工作助手限界上下文
│   ├── domain/                          # 领域层
│   │   ├── entity/
│   │   │   ├── CaseEntry.java          # 聚合根
│   │   │   └── TodoItem.java           # 聚合根
│   │   ├── valueobject/
│   │   │   ├── CaseId.java
│   │   │   └── TodoId.java
│   │   ├── service/
│   │   │   └── CaseDomainService.java
│   │   ├── port/
│   │   │   └── SemanticSearchPort.java # 语义搜索端口
│   │   └── repository/
│   │       ├── CaseRepository.java
│   │       └── TodoRepository.java
│   │
│   ├── application/                     # 应用层
│   │   ├── service/
│   │   │   ├── CaseApplicationService.java
│   │   │   ├── CaseExcelService.java
│   │   │   └── TodoApplicationService.java
│   │   └── dto/
│   │       ├── CaseInputDTO.java
│   │       ├── CaseOutputDTO.java
│   │       ├── TodoInputDTO.java
│   │       └── TodoOutputDTO.java
│   │
│   ├── infrastructure/                  # 基础设施层
│   │   ├── adapter/
│   │   │   ├── PythonSemanticSearchAdapter.java  # Python语义搜索适配器
│   │   │   └── SimpleSemanticSearchAdapter.java  # 降级方案
│   │   ├── config/
│   │   │   └── DomainServiceConfig.java
│   │   └── persistence/
│   │       ├── entity/
│   │       │   ├── CaseEntryPO.java
│   │       │   └── TodoItemPO.java
│   │       └── repository/
│   │           ├── CaseEntryJpaRepository.java
│   │           ├── CaseRepositoryImpl.java
│   │           ├── TodoItemJpaRepository.java
│   │           └── TodoRepositoryImpl.java
│   │
│   └── interfaces/                      # 接口层
│       └── rest/
│           ├── CaseController.java
│           ├── TodoController.java
│           ├── ChatController.java          # 智能问答接口
│           └── GlobalExceptionHandler.java
│
└── monitor/                             # 系统监控限界上下文
    ├── domain/                          # 领域层
    │   ├── entity/
    │   │   ├── MonitorSnapshot.java    # 聚合根
    │   │   └── ProcessInfo.java        # 实体
    │   │   valueobject/
    │   │   ├── SnapshotId.java
    │   │   ├── SystemInfo.java
    │   │   ├── CpuMetrics.java
    │   │   ├── MemoryMetrics.java
    │   │   ├── DiskMetrics.java
    │   │   └── NetworkMetrics.java
    │   ├── port/
    │   │   └── SystemMonitorPort.java  # 系统监控端口
    │   └── service/
    │       └── MonitorDomainService.java
    │
    ├── application/                     # 应用层
    │   ├── service/
    │   │   ├── MonitorApplicationService.java
    │   │   └── MonitorExcelService.java
    │   ├── dto/
    │   │   ├── MonitorSnapshotDTO.java
    │   │   ├── SystemInfoDTO.java
    │   │   └── ProcessInfoDTO.java
    │   └── assembler/
    │       └── MonitorAssembler.java
    │
    ├── infrastructure/                  # 基础设施层
    │   ├── adapter/
    │   │   └── OshiSystemMonitorAdapter.java  # OSHI适配器
    │   └── scheduler/
    │       └── MonitorScheduler.java        # 定时采集
    │
    └── interfaces/                      # 接口层
        └── rest/
            └── MonitorController.java
```

## 4. 限界上下文说明

### 4.1 日志分析上下文 (Log Context)

**核心领域**：日志数据的接收、清洗、分析与存储

**聚合根**：`LogEntry`
- 日志条目的核心实体
- 包含原始内容、清洗后内容、日志级别等

**领域服务**：
- `LogCleansingService`: 日志清洗服务，支持多种清洗策略
- `LogAnalysisService`: 日志分析服务，统计错误率等
- `LogLineDetector`: 日志行检测服务

**值对象**：
- `LogId`: 日志唯一标识
- `LogLevel`: 日志级别（ERROR, WARN, INFO, DEBUG, TRACE）
- `LogSource`: 日志来源（应用名、环境）
- `CleansingResult`: 清洗结果

### 4.2 工作助手上下文 (Assistant Context)

**核心领域**：案例库管理和待办事项管理

**聚合根**：
- `CaseEntry`: 案例条目
- `TodoItem`: 待办事项

**领域服务**：
- `CaseDomainService`: 案例去重检查等业务规则

**值对象**：
- `CaseId`: 案例唯一标识
- `TodoId`: 待办事项唯一标识

**端口**：
- `SemanticSearchPort`: 语义搜索接口（由基础设施层实现）

### 4.3 系统监控上下文 (Monitor Context)

**核心领域**：系统指标采集、进程监控、历史数据管理

**聚合根**：`MonitorSnapshot`
- 系统监控快照，包含某个时间点的完整系统信息
- 聚合CPU、内存、磁盘、网络指标和进程信息

**领域服务**：
- `MonitorDomainService`: 监控数据聚合和计算

**值对象**：
- `SnapshotId`: 快照唯一标识
- `SystemInfo`: 系统基础信息（操作系统、架构、启动时间）
- `CpuMetrics`: CPU指标（使用率、核心数、进程数）
- `MemoryMetrics`: 内存指标（总量、已用、可用）
- `DiskMetrics`: 磁盘指标（总空间、已用空间）
- `NetworkMetrics`: 网络指标（上行/下行流量）

**端口**：
- `SystemMonitorPort`: 系统监控接口（由OSHI适配器实现）

### 4.4 共享内核 (Shared Kernel)

包含三个限界上下文共同使用的代码：
- `PageDTO`: 通用分页响应DTO
- `WebConfig`: Web层公共配置
- `ExcelBuilder`: 通用Excel导出工具类（建造者模式）
- `PythonBridge`: Java-Python集成基础设施（Py4J封装）

## 5. 依赖关系

```
┌────────────────────────────────────────────────────────┐
│                      shared (共享内核)                   │
└────────────────────────────────────────────────────────┘
                ▲                          ▲
                │                          │
    ┌───────────┴───────────┐  ┌───────────┴───────────┐
    │    log (日志分析)      │  │  assistant (工作助手)  │
    └───────────────────────┘  └───────────────────────┘
```

- `log`、`assistant` 和 `monitor` 是三个独立的限界上下文，彼此不直接依赖
- 三者都可以使用 `shared` 中的公共代码
- 每个上下文内部遵循DDD分层架构

## 6. API端点

### 6.1 日志分析API

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/v1/logs/upload | 上传日志文件 |
| GET | /api/v1/logs | 获取日志列表（分页） |
| GET | /api/v1/logs/{id} | 获取单条日志 |
| DELETE | /api/v1/logs/{id} | 删除日志 |
| GET | /api/v1/logs/statistics | 获取统计信息 |
| GET | /api/v1/logs/search | 搜索日志 |
| GET | /api/v1/logs/level/{level} | 按级别筛选 |

### 6.2 工作助手API

**案例管理**
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/cases | 添加案例（带去重） |
| PUT | /api/cases/{id} | 更新案例 |
| DELETE | /api/cases/{id} | 删除案例 |
| GET | /api/cases | 获取案例列表（分页） |
| GET | /api/cases/{id} | 获取单个案例 |
| GET | /api/cases/module/{module} | 按模块获取案例 |
| GET | /api/cases/modules | 获取所有模块 |
| GET | /api/cases/search | 搜索案例 |
| GET | /api/cases/export | 导出Excel |
| POST | /api/cases/import | 导入Excel |

**待办管理**
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/todos | 添加待办 |
| PUT | /api/todos/{id} | 更新待办 |
| DELETE | /api/todos/{id} | 删除待办 |
| GET | /api/todos/today | 获取今日待办 |
| PATCH | /api/todos/{id}/toggle | 切换完成状态 |

**智能问答**
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/chat | 发送消息获取智能回复 |

### 6.3 系统监控API

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/monitor/current | 获取当前系统信息 |
| GET | /api/monitor/history | 获取历史监控数据（分页） |
| GET | /api/monitor/export | 导出Excel监控报表 |

## 7. 业务规则

### 7.1 日志清洗
- 支持多种清洗策略：标准日志、JSON日志、简单文本
- 自动检测日志格式并选择合适的策略
- 清洗过程提取：时间戳、日志级别、消息内容

### 7.2 案例去重
- **规则**：同一模块下不允许相同标题的案例
- 添加时检测重复会返回400错误
- 导入时已存在的案例会自动更新而非报错

### 7.3 语义搜索
- 基于TF-IDF变体的简单向量匹配
- 使用余弦相似度计算文本相似性
- 返回Top 3语义匹配结果

## 8. 技术栈

- **框架**: Spring Boot 3.2.0, Spring Data JPA
- **语言**: Java 21, Python 3.x
- **数据库**: MySQL 8.0
- **构建**: Maven 3.6+
- **Java-Python集成**: Py4J 0.10.9.7
- **语义模型**: sentence-transformers (Python)
- **其他**: Lombok, Apache POI (Excel处理)

## 9. Py4J集成架构

### 9.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Java端 (生产者)                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  CaseApplicationService / CaseExcelService               ││
│  │  - 提交语义计算任务 → 等待任务完成                          ││
│  └────────────────────────────┬────────────────────────────┘│
│  ┌────────────────────────────▼────────────────────────────┐│
│  │         shared.infrastructure.python                      ││
│  │  ┌──────────────┐ ┌────────────────┐ ┌────────────────┐ ││
│  │  │PythonTaskQueue│ │ProcessManager  │ │GatewayServer   │ ││
│  │  │任务队列+等待   │ │进程池+动态扩缩  │ │Py4J网关        │ ││
│  │  └──────────────┘ └────────────────┘ └────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────┬──────────────────────────────┘
                               │ Py4J
┌──────────────────────────────▼──────────────────────────────┐
│                       Python端 (消费者)                       │
│  semantic_worker.py: 获取任务 → 计算语义向量 → 返回结果          │
└─────────────────────────────────────────────────────────────┘
```

### 9.2 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| PythonTask | shared | 任务抽象，包含等待/通知机制 |
| PythonTaskQueue | shared | 线程安全任务队列 |
| PythonProcessManager | shared | Python进程池管理，动态扩缩容 |
| PythonGatewayServer | shared | Py4J网关服务器 |
| PythonBridge | shared | 高级API，提交任务并等待结果 |
| semantic_worker.py | python/ | Python端语义处理Worker |

### 9.3 任务流程

1. Java端提交任务到PythonTaskQueue
2. Python Worker通过Py4J轮询获取任务
3. Python计算语义向量（使用sentence-transformers）
4. Python通过Py4J返回结果
5. Java端通过CountDownLatch等待通知
6. 结果保存到数据库

## 10. 扩展点

1. **日志清洗策略**：实现 `LogCleansingStrategy` 接口添加新策略
2. **语义搜索**：实现 `SemanticSearchPort` 接口替换搜索算法
3. **Python语义模型**：修改 `semantic_worker.py` 中的模型
4. **新的限界上下文**：按相同模式添加新的业务领域
