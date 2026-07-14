package com.oraculum.llm.exception;

public class LlmMissingTierConfigurationException extends RuntimeException {
    public LlmMissingTierConfigurationException(String message) {
        super(message);
    }
}
