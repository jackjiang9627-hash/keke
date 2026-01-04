package com.keke.assistant.domain.exception;

/**
 * 案例重复异常
 */
public class CaseDuplicateException extends CaseException {
    
    private final String title;
    private final String moduleName;
    
    public CaseDuplicateException(String title, String moduleName) {
        super(String.format("案例已存在：模块[%s]下已有同名标题[%s]", moduleName, title));
        this.title = title;
        this.moduleName = moduleName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getModuleName() {
        return moduleName;
    }
}
