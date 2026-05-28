package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.LlmRequestDto;
import com.oraculum.llm.api.dto.LlmResponseDto;

public interface LlmApi {

    LlmResponseDto complete(LlmRequestDto request);
}
