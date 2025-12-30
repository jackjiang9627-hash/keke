package com.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 日志清洗与分析系统 - 启动类
 * 
 * 基于DDD（领域驱动设计）架构
 * 
 * 架构分层：
 * ├── domain          (领域层) - 核心业务逻辑
 * │   ├── entity      - 实体/聚合根
 * │   ├── valueobject - 值对象
 * │   ├── service     - 领域服务
 * │   └── repository  - 仓储接口
 * ├── application     (应用层) - 用例编排
 * │   ├── service     - 应用服务
 * │   ├── dto         - 数据传输对象
 * │   └── assembler   - DTO组装器
 * ├── infrastructure  (基础设施层) - 技术实现
 * │   ├── persistence - 持久化实现
 * │   ├── cleansing   - 清洗策略实现
 * │   └── config      - 配置类
 * └── interfaces      (接口层) - 对外暴露
 *     └── rest        - REST API
 */
@SpringBootApplication
public class KekeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KekeApplication.class, args);
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════╗
            ║                                                           ║
            ║     个人工作助手已启动 (keke)                                ║
            ║     基于DDD架构设计                                         ║
            ║                                                           ║
            ║     API地址: http://localhost:8080/                        ║
            ║                                                           ║
            ║                                                           ║
            ╚═══════════════════════════════════════════════════════════╝
            """);
    }
}
