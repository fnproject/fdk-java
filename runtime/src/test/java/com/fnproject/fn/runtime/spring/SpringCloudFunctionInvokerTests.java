package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.*;
import com.fnproject.fn.runtime.spring.testfns.FunctionConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.ClassUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerTests {
    @Test
    @Ignore
    public void shouldInvokeFunction() throws IOException {
        MethodWrapper method = new DefaultMethodWrapper(FunctionConfig.class, ClassUtils.getMethod(FunctionConfig.class, "upperCaseFunction"));
        InvocationContext ctx = new InvocationContext() {
            FunctionRuntimeContext runtimeContex = new FunctionRuntimeContext(method, Collections.emptyMap());

            @Override
            public RuntimeContext getRuntimeContext() {
                return runtimeContex;
            }

            @Override
            public void addListener(InvocationListener listener) {
                throw new IllegalStateException("Cannot add listeners in a test context");
            }
        };
        SpringCloudFunctionInvoker invoker = new SpringCloudFunctionInvoker(ctx.getRuntimeContext());

        InputEvent input = new ReadOnceInputEvent("TEST-app", "/TEST-route",
                "www.TEST.com/TEST-route/", "POST", new ByteArrayInputStream(new byte[0]),
                Headers.emptyHeaders(), new QueryParametersImpl());
        Optional<OutputEvent> output = invoker.tryInvoke(ctx, input);

        assertThat(output.isPresent()).isTrue();
        assertThat(output.get().isSuccess()).isTrue();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        output.get().writeToOutput(os);
        String outputBody = new String(os.toByteArray());
        assertThat(outputBody).isEqualTo("John Doe");
    }
}

