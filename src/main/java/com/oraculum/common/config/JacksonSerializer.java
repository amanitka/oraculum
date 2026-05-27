package com.oraculum.common.config;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

public class JacksonSerializer implements Serializer<Object> {

    private final JsonMapper mapper;

    public JacksonSerializer(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) return null;
        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize to JSON for topic " + topic, e);
        }
    }
}
