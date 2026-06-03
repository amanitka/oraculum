package com.oraculum.analyst.config;

import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.domain.AgentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AgentConfig {

    @Bean
    public Map<AgentType, AgentService<?>> agents(List<AgentService<?>> agentServiceList) {
        return agentServiceList.stream()
                .collect(Collectors.toMap(AgentService::getName,
                        Function.identity(),
                        (a1, a2) -> a1,
                        () -> new EnumMap<>(AgentType.class)));
    }
}