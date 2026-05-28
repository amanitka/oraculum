package com.oraculum.llm.api.dto;

import java.util.Map;

public sealed interface LlmResponseFormatDto permits LlmResponseFormatDto.JsonSchema, LlmResponseFormatDto.Raw {

    record JsonSchema(Class<?> schemaType) implements LlmResponseFormatDto {
    }

    record Raw(Map<String, Object> raw) implements LlmResponseFormatDto {
    }
}
