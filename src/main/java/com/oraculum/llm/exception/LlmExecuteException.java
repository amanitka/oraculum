package com.oraculum.llm.exception;

public class LlmExecuteException extends RuntimeException {
    public LlmExecuteException(String message, Exception e) {
        super(message, e);
    }
}
