package com.keke.log.domain.valueobject;

import java.util.Objects;

/**
 * 日志来源 - 值对象
 * 
 * DDD概念：值对象（Value Object）
 * - 描述日志的来源信息
 * - 不可变对象
 */
public final class LogSource {

    private final String application;  // 应用名称
    private final String host;         // 主机地址
    private final String environment;  // 环境（dev/test/prod）

    public LogSource(String application, String host, String environment) {
        this.application = application != null ? application : "unknown";
        this.host = host != null ? host : "unknown";
        this.environment = environment != null ? environment : "unknown";
    }

    public String getApplication() {
        return application;
    }

    public String getHost() {
        return host;
    }

    public String getEnvironment() {
        return environment;
    }

    /**
     * 生成完整的来源标识
     */
    public String getFullIdentifier() {
        return String.format("%s@%s[%s]", application, host, environment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogSource logSource = (LogSource) o;
        return Objects.equals(application, logSource.application) &&
               Objects.equals(host, logSource.host) &&
               Objects.equals(environment, logSource.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(application, host, environment);
    }

    @Override
    public String toString() {
        return getFullIdentifier();
    }
}
