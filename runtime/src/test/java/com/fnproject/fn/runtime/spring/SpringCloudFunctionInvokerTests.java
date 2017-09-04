package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerTests {
    @Test
    public void shouldAutoWireBeans() throws IOException {
        SpringCloudFunctionInvoker invoker = new SpringCloudFunctionInvoker(FunctionConfig.class);
        InvocationContext ctx = new InvocationContext() {
            FunctionRuntimeContext runtimeContex = new FunctionRuntimeContext(new FnFunction(FunctionConfig.class.getCanonicalName(), "upperCase"), Collections.emptyMap());

            @Override
            public RuntimeContext getRuntimeContext() {
                return runtimeContex;
            }

            @Override
            public void addListener(InvocationListener listener) {
                throw new IllegalStateException("Cannot add listeners in a test context");
            }
        };

        InputEvent input = new ReadOnceInputEvent("TEST-app", "/TEST-route",
                "www.TEST.com/TEST-route/", "POST", new ByteArrayInputStream(new byte[0]),
                Headers.emptyHeaders(), new QueryParametersImpl());
        Optional<OutputEvent> output = invoker.tryInvoke(ctx, input);

        assertThat(output.isPresent());
        assertThat(output.get().isSuccess()).isTrue();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        output.get().writeToOutput(os);
        String outputBody = new String(os.toByteArray());
        assertThat(outputBody).isEqualTo("John Doe");
    }
}

class FunctionConfig {
    @Autowired
    private Name name;

    @Bean
    public Supplier<String> upperCase() {
        return () -> name.first + " " + name.last;
    }

    @Bean
    public Name defaultName()  {
        return new Name("John", "Doe");
    }
}

class Name {
    public final String first;
    public final String last;

    public Name(String first, String last) {
        this.first = first;
        this.last = last;
    }
}
