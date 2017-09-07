package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.FunctionLoader;
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import com.fnproject.fn.runtime.QueryParametersImpl;
import com.fnproject.fn.runtime.ReadOnceInputEvent;
import com.fnproject.fn.runtime.spring.function.SpringCloudFunction;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.Not;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SpringCloudFunctionInvokerTests {
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
