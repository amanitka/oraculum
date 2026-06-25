package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CitationRegistry {
    
    // Maps unique source ID (e.g. "IncomeStatementDto_123") to citation ID (e.g. 1)
    @JsonIgnore
    private final Map<String, Integer> sourceIdToCitationId = new HashMap<>();
    
    // Maps citation ID (e.g. 1) to the actual data payload or metadata for UI rendering
    private final Map<Integer, Object> citations = new HashMap<>();
    
    @JsonIgnore
    private int nextId = 1;

    public int getOrAssignCitationId(String sourceUniqueId, Object sourceData) {
        if (sourceIdToCitationId.containsKey(sourceUniqueId)) {
            return sourceIdToCitationId.get(sourceUniqueId);
        }
        int id = nextId++;
        sourceIdToCitationId.put(sourceUniqueId, id);
        citations.put(id, sourceData);
        return id;
    }

    public String getOrAssignCitationId(Class<?> clazz, Object id, Object sourceData) {
        String sourceUniqueId = clazz.getSimpleName() + "_" + id;
        return String.valueOf(getOrAssignCitationId(sourceUniqueId, sourceData));
    }
}
