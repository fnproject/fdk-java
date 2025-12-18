package com.fnproject.events.coercion.jackson;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64ToTypeDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private final JavaType targetType;

    public Base64ToTypeDeserializer() {
        this(null);
    }

    private Base64ToTypeDeserializer(JavaType targetType) {
        this.targetType = targetType;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final JavaType t = (targetType != null) ? targetType : ctxt.constructType(Object.class);
        final ObjectCodec codec = p.getCodec();
        final ObjectMapper mapper = (codec instanceof ObjectMapper) ? (ObjectMapper) codec : new ObjectMapper();

        JsonToken tok = p.currentToken();
        if (tok == JsonToken.VALUE_NULL) {
            return null;
        }

        // If we didn't receive a string, just let Jackson handle it normally (it might already be JSON)
        if (tok != JsonToken.VALUE_STRING) {
            return codec.readValue(p, t);
        }

        // We have a string; attempt base64 decode first
        String v = p.getValueAsString();
        if (v == null) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(v);

            // If T is String, return textual content of decoded bytes
            if (t.isTypeOrSubTypeOf(String.class)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }

            // Otherwise, treat decoded bytes as JSON and map to target type T
            try (JsonParser jp = mapper.getFactory().createParser(bytes)) {
                return mapper.readValue(jp, t);
            }
        } catch (IllegalArgumentException notBase64) {
            // Not valid base64; fallback behavior

            // If T is String, return the raw string as-is
            if (t.isTypeOrSubTypeOf(String.class)) {
                return v;
            }

            // Try to interpret the string itself as JSON for T
            // (common case: string contains JSON payload but wasn't base64-encoded)
            try {
                return mapper.readValue(v, t);
            } catch (IOException cannotParseAsJson) {
                // Final fallback: wrap as JSON string and let Jackson coerce if possible (e.g., to primitive/wrapper)
                // If not compatible, throw a Jackson-standard exception
                try {
                    return mapper.readValue(mapper.writeValueAsBytes(v), t);
                } catch (IOException e) {
                    return ctxt.reportInputMismatch(
                        t,
                        "Cannot coerce non-base64 string value into %s: %s",
                        t.toString(), e.getMessage()
                    );
                }
            }
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType t = (property != null) ? property.getType() : ctxt.getContextualType();
        if (t == null || t.hasRawClass(Object.class)) {
            // On constructor params this often mirrors property.getType(); if still Object,
            // try the context’s parent (the bean being created) via getContextualType()
            JavaType enclosing = ctxt.getContextualType();
            if (enclosing != null && enclosing.containedTypeCount() > 0) {
                t = enclosing.containedType(0); // StreamingData<T> -> T
            }
            if (t == null || t.hasRawClass(Object.class)) {
                t = ctxt.constructType(Object.class);
            }
        }

        return new Base64ToTypeDeserializer(t);

    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt);
    }

}  