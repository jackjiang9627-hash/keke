package com.loganalyzer.assistant.domain.valueobject;

import java.util.UUID;

/**
 * 案例ID值对象
 */
public record CaseId(String value) {
    
    public static CaseId generate() {
        return new CaseId(UUID.randomUUID().toString());
    }
    
    public static CaseId of(String value) {
        return new CaseId(value);
    }
}
