package com.smarturl.hub.link.config;

import java.lang.reflect.Type;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

    private final ObjectMapper mapper;

    public Jackson3JsonFormatMapper() {
        this(JsonMapper.builder().build());
    }

    public Jackson3JsonFormatMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T fromString(CharSequence charSequence, Type type) {
        return (T) mapper.readValue(charSequence.toString(), mapper.constructType(type));
    }

    @Override
    protected <T> String toString(T value, Type type) {
        return mapper.writerFor(mapper.constructType(type)).writeValueAsString(value);
    }
}
