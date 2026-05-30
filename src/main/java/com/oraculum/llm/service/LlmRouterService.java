package com.oraculum.llm.service;

public interface LlmRouterService {
    <T> T generate(String tier, String prompt, Class<T> responseType);
}
