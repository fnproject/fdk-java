package com.fnproject.springframework.function;

import com.fnproject.springframework.function.functions.SpringCloudFunction;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringCloudFunctionInvokerTest {
    private SpringCloudFunctionInvoker invoker;

    @Before
    public void setUp() {
        invoker = new SpringCloudFunctionInvoker((SpringCloudFunctionLoader) null);
    }

    @Test
    public void invokesFunctionWithEmptyFlux() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[0]);

        assertThat(result).isNull();
    }

    @Test
    public void invokesFunctionWithFluxOfSingleItem() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[]{ "hello" });

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    public void invokesFunctionWithFluxOfMultipleItems() {
        SpringCloudFunction fnWrapper = new SpringCloudFunction(x -> x, new SimpleFunctionInspector());

        Object result = invoker.tryInvoke(fnWrapper, new Object[]{ Arrays.asList("hello", "world") });

        assertThat(result).isInstanceOf(List.class);
        assertThat((List) result).containsSequence("hello", "world");
    }

}
