package com.oraculum.analyst.agent.service;

import com.oraculum.analyst.api.domain.AgentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CriticAgentTest {

    @Test
    void testAgentType() {
        // Just a basic placeholder test
        assertEquals(AgentType.CRITIC, AgentType.CRITIC);
    }
}
