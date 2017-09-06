package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.runtime.DefaultMethodWrapper;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.runtime.FunctionRuntimeContext;
import com.fnproject.fn.runtime.exception.InvalidEntryPointException;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SpringCloudFunctionTests {
    @Test
    public void shouldLoadFunctionSpecifiedInContextOverDefault() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        stubContextToReturnFunction(context, "lowerCaseFunction", (Function<String, String>) String::toLowerCase);
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_SUPPLIER_NAME)).thenReturn(null);
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_CONSUMER_NAME)).thenReturn(null);

        SpringCloudFunction discovery = new SpringCloudFunction(context);

        discovery.discover();
        assertThatFunctionIsFound(discovery);

        Object result = discovery.getFunction().apply("HELLO");

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    public void shouldLoadSupplierSpecifiedInContextOverDefault() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        String supplierOutput = "overriding supplier output";
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_FUNCTION_NAME)).thenReturn(null);
        stubContextToReturnSupplier(context, "overridingSupplier", (Supplier<String>) () -> supplierOutput);
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_CONSUMER_NAME)).thenReturn(null);
        SpringCloudFunction discovery = new SpringCloudFunction(context);

        discovery.discover();
        assertThatSupplierIsFound(discovery);

        Object result = discovery.getSupplier().get();

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo(supplierOutput);
    }

    @Test
    public void shouldLoadConsumerSpecifiedInContextOverDefault() throws Exception {
        final String[] consumerInput = new String[1];
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_FUNCTION_NAME)).thenReturn(null);
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_SUPPLIER_NAME)).thenReturn(null);
        stubContextToReturnConsumer(context, "overridingConsumer", (Consumer<String>) (str) -> consumerInput[0] = str);
        SpringCloudFunction discovery = new SpringCloudFunction(context);

        discovery.discover();
        assertThatConsumerIsFound(discovery);

        discovery.getConsumer().accept("Hello");

        assertThat(consumerInput[0]).isEqualTo("Hello");
    }

    @Test
    public void shouldLoadFunctionInPreferenceToSupplierAndConsumerWhenAllPropertiesArePresent() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        stubContextToReturnFunction(context, "overridingFunction", (Function<String, String>) String::toLowerCase);
        stubContextToReturnConsumer(context, "overridingConsumer", (Consumer<String>) System.out::println);
        stubContextToReturnSupplier(context, "overridingSupplier", (Supplier<String>) () -> "hello");
        SpringCloudFunction discovery = new SpringCloudFunction(context);

        discovery.discover();
        assertThatFunctionIsFound(discovery);
    }

    @Test
    public void shouldLoadConsumerInPreferenceToSupplierWhenBothAreSetInProperties() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        stubContextToReturnConsumer(context, "overridingConsumer", (Consumer<String>) System.out::println);
        stubContextToReturnSupplier(context, "overridingSupplier", (Supplier<String>) () -> "hello");
        SpringCloudFunction discovery = new SpringCloudFunction(context);

        discovery.discover();
        assertThatConsumerIsFound(discovery);
    }

    private void stubContextToReturnFunction(ApplicationContext context, String fnName, Function<?, ?> fn) {
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_FUNCTION_NAME)).thenReturn(fnName);
        when((Object) context.getBean(fnName, Function.class)).thenReturn(fn);
    }

    private void stubContextToReturnSupplier(ApplicationContext context, String supplierName, Supplier<?> supplier) {
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_SUPPLIER_NAME)).thenReturn(supplierName);
        when((Object) context.getBean(supplierName, Supplier.class)).thenReturn(supplier);
    }

    private void stubContextToReturnConsumer(ApplicationContext context, String consumerName, Consumer<?> consumer) {
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_CONSUMER_NAME)).thenReturn(consumerName);
        when((Object) context.getBean(consumerName, Consumer.class)).thenReturn(consumer);
    }

    @Test(expected = InvalidEntryPointException.class)
    @SuppressWarnings({"CheckReturnValue"})
    public void shouldThrowErrorIfFunctionSpecifiedInContextIsNotAvailable() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class, Mockito.RETURNS_DEEP_STUBS);
        String overridingFunctionName = "lowerCaseFunction";
        when(context.getEnvironment().getProperty(SpringCloudFunction.PROPERTY_KEY_FUNCTION_NAME)).thenReturn(overridingFunctionName);
        when((Object) context.getBean(overridingFunctionName, Function.class)).thenThrow(
                new NoSuchBeanDefinitionException("Bean for " + overridingFunctionName + " could not be found (Expected exception)"));

        SpringCloudFunction discovery = new SpringCloudFunction(context);
        discovery.discover();

        discovery.getFunction().apply("HELLO");
    }

    private void assertThatFunctionIsFound(SpringCloudFunction discovery) {
        assertThat(discovery.getFunction()).isInstanceOf(Function.class);
        assertThat(discovery.getConsumer()).isNull();
        assertThat(discovery.getSupplier()).isNull();
    }

    private void assertThatSupplierIsFound(SpringCloudFunction discovery) {
        assertThat(discovery.getFunction()).isNull();
        assertThat(discovery.getConsumer()).isNull();
        assertThat(discovery.getSupplier()).isInstanceOf(Supplier.class);
    }


    private void assertThatConsumerIsFound(SpringCloudFunction discovery) {
        assertThat(discovery.getFunction()).isNull();
        assertThat(discovery.getConsumer()).isInstanceOf(Consumer.class);
        assertThat(discovery.getSupplier()).isNull();
    }

    private RuntimeContext fromMethod(Class<?> cls, String methodName) {
        return new FunctionRuntimeContext(new DefaultMethodWrapper(cls, methodName), new HashMap<>());
    }

}
