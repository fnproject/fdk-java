package com.fnproject.springframework.function;

import com.fnproject.springframework.function.functions.SpringCloudMethod;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.function.context.catalog.InMemoryFunctionCatalog;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringCloudFunctionLoaderTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private SpringCloudFunctionLoader loader;

    @Mock
    private InMemoryFunctionCatalog catalog;

    @Before
    public void setUp() {
        loader = new SpringCloudFunctionLoader(catalog, null);
        setUpCatalogToReturnNullForLookupByDefault();
    }

    private void setUpCatalogToReturnNullForLookupByDefault() {
        when(catalog.lookup(any(), any())).thenReturn(null);
    }

    @Test
    public void shouldLoadFunctionBeanCalledFunction() {
        Function<Object, Object> fn = (x) -> x;
        stubCatalogToReturnFunction(fn);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(fn.getClass());
    }

    @Test
    public void shouldLoadConsumerBeanCalledConsumerIfFunctionNotAvailable() {
        Consumer<Object> consumer = (x) -> {
        };
        stubCatalogToReturnConsumer(consumer);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(consumer.getClass());
    }

    @Test
    public void shouldLoadSupplierBeanCalledSupplierIfNoConsumerOrFunctionAvailable() {
        Supplier<Object> supplier = () -> "x";
        stubCatalogToReturnSupplier(supplier);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(supplier.getClass());
    }

    @Test
    public void shouldLoadUserSpecifiedSupplierInEnvVarOverDefaultFunction() {
        String supplierBeanName = "mySupplier";
        Supplier<Object> supplier = () -> "x";
        Function<Object, Object> function = (x) -> x;

        setSupplierEnvVar(supplierBeanName);
        stubCatalogToReturnFunction(function);
        stubCatalogToReturnSupplier(supplierBeanName, supplier);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(supplier.getClass());
    }


    @Test
    public void shouldLoadUserSpecifiedConsumerInEnvVarOverDefaultFunction() {
        String beanName = "myConsumer";
        Consumer<Object> consumer = (x) -> {
        };
        Function<Object, Object> function = (x) -> x;

        setConsumerEnvVar(beanName);
        stubCatalogToReturnFunction(function);
        stubCatalogToReturnConsumer(beanName, consumer);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(consumer.getClass());
    }

    @Test
    public void shouldLoadUserSpecifiedFunctionInEnvVarOverDefaultFunction() {
        String functionBeanName = "myFunction";
        Function<Object, Object> defaultFunction = (x) -> x;
        Function<Object, Object> myFunction = (x) -> x.toString();

        setFunctionEnvVar(functionBeanName);
        stubCatalogToReturnFunction(defaultFunction);
        stubCatalogToReturnFunction(functionBeanName, myFunction);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(myFunction.getClass());
    }

    private void stubCatalogToReturnFunction(String beanName, Function<Object, Object> function) {
        when(catalog.lookup(Function.class, beanName)).thenReturn(function);
    }

    private void stubCatalogToReturnConsumer(String beanName, Consumer<Object> consumer) {
        when(catalog.lookup(Consumer.class, beanName)).thenReturn(consumer);
    }

    private void stubCatalogToReturnSupplier(String beanName, Supplier<Object> supplier) {
        when(catalog.lookup(Supplier.class, beanName)).thenReturn(supplier);
    }

    private void stubCatalogToReturnSupplier(Supplier<Object> supplier) {
        stubCatalogToReturnSupplier("supplier", supplier);
    }

    private void stubCatalogToReturnFunction(Function<Object, Object> function) {
        stubCatalogToReturnFunction("function", function);
    }

    private void stubCatalogToReturnConsumer(Consumer<Object> consumer) {
        stubCatalogToReturnConsumer("consumer", consumer);
    }

    private void setFunctionEnvVar(String beanName) {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_FUNCTION_NAME, beanName);
    }

    private void setConsumerEnvVar(String beanName) {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_CONSUMER_NAME, beanName);
    }

    private void setSupplierEnvVar(String supplierBeanName) {
        environmentVariables.set(SpringCloudFunctionLoader.ENV_VAR_SUPPLIER_NAME, supplierBeanName);
    }

    private SpringCloudMethod getDiscoveredFunction() {
        loader.loadFunction();
        return loader.getFunction();
    }

}
