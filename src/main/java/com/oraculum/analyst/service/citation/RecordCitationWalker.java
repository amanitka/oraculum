package com.oraculum.analyst.service.citation;

import com.oraculum.analyst.agent.dto.AgentEvidence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Component
@Slf4j
public class RecordCitationWalker {

    public <T> T walkRecord(T record,
                            UnaryOperator<String> textVerifier,
                            Function<AgentEvidence, AgentEvidence> evidenceVerifier) {
        if (record == null || !record.getClass().isRecord()) {
            return record;
        }
        if (record instanceof AgentEvidence ev) {
            return (T) evidenceVerifier.apply(ev);
        }
        try {
            return buildVerifiedRecord(record, record.getClass(), textVerifier, evidenceVerifier);
        } catch (Exception e) {
            log.error("Failed to verify record citations for {}", record.getClass().getName(), e);
            return record;
        }
    }

    private <T> T buildVerifiedRecord(T record,
                                      Class<?> clazz,
                                      UnaryOperator<String> textVerifier,
                                      Function<AgentEvidence, AgentEvidence> evidenceVerifier) throws Exception {
        RecordComponent[] components = clazz.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            Method accessor = components[i].getAccessor();
            args[i] = verifyComponentValue(accessor.invoke(record), textVerifier, evidenceVerifier);
        }
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return (T) constructor.newInstance(args);
    }

    private Object verifyComponentValue(Object value,
                                        UnaryOperator<String> textVerifier,
                                        Function<AgentEvidence, AgentEvidence> evidenceVerifier) {
        if (value instanceof String str) {
            return textVerifier.apply(str);
        }
        if (value instanceof List<?> list) {
            return verifyListCitations(list, textVerifier, evidenceVerifier);
        }
        if (value != null && value.getClass().isRecord()) {
            return walkRecord(value, textVerifier, evidenceVerifier);
        }
        return value;
    }

    private List<Object> verifyListCitations(List<?> list,
                                             UnaryOperator<String> textVerifier,
                                             Function<AgentEvidence, AgentEvidence> evidenceVerifier) {
        List<Object> verifiedList = new ArrayList<>(list.size());
        for (Object item : list) {
            verifiedList.add(verifyComponentValue(item, textVerifier, evidenceVerifier));
        }
        return verifiedList;
    }
}
