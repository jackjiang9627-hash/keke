package com.keke.assistant.domain.valueobject;

import java.util.UUID;

/**
 * Todo项ID值对象
 */
public record TodoId(String value) {
    
    public static TodoId generate() {
        return new TodoId(UUID.randomUUID().toString());
    }
    
    public static TodoId of(String value) {
        return new TodoId(value);
    }
}
