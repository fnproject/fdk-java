package com.fnproject.fn.runtime.spring;

import com.fnproject.fn.runtime.spring.function.SpringCloudMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.function.registry.FunctionCatalog;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringCloudFunctionLoaderTests {
    private SpringCloudFunctionLoader loader;
    @Mock
    private FunctionCatalog catalog;

    @Before
    public void setUp() {
        loader = new SpringCloudFunctionLoader(catalog, null);
        setUpCatalogToReturnNullForLookupByDefault();
        clearFunctionSystemProperties();
    }

    private void clearFunctionSystemProperties() {
        System.clearProperty("function.name");
        System.clearProperty("consumer.name");
        System.clearProperty("supplier.name");
    }

    private void setUpCatalogToReturnNullForLookupByDefault() {
        when(catalog.lookupFunction(any())).thenReturn(null);
        when(catalog.lookupConsumer(any())).thenReturn(null);
        when(catalog.lookupSupplier(any())).thenReturn(null);
    }

    @Test
    public void shouldLoadFunctionBeanCalledFunction() {
        Function<Object, Object> fn = (x) -> x;
        stubCatalogToReturnFunction(fn);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(fn.getClass());
    }

    @Test
    public void shouldLoadConsumerBeanCalledConsumerIfFunctionNotAvailable() {
        Consumer<Object> consumer = (x) -> {};
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
    public void shouldLoadUserSpecifiedSupplierInSystemPropsOverDefaultFunctionIsSpecified() {
        String supplierBeanName = "mySupplier";
        Supplier<Object> supplier = () -> "x";
        Function<Object, Object> function = (x) -> x;

        System.setProperty("supplier.name", supplierBeanName);
        stubCatalogToReturnFunction(function);
        stubCatalogToReturnSupplier(supplierBeanName, supplier);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(supplier.getClass());
    }

    @Test
    public void shouldLoadUserSpecifiedConsumerInSystemPropsOverDefaultFunctionIsSpecified() {
        String beanName = "myConsumer";
        Consumer<Object> consumer = (x) -> {};
        Function<Object, Object> function = (x) -> x;

        System.setProperty("consumer.name", beanName);
        stubCatalogToReturnFunction(function);
        stubCatalogToReturnConsumer(beanName, consumer);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(consumer.getClass());
    }

    @Test
    public void shouldLoadUserSpecifiedFunctionOverDefaultFunction() {
        String functionBeanName = "myFunction";
        Function<Object, Object> defaultFunction = (x) -> x;
        Function<Object, Object> myFunction = (x) -> x.toString();

        System.setProperty("function.name", functionBeanName);
        stubCatalogToReturnFunction(defaultFunction);
        stubCatalogToReturnFunction(functionBeanName, myFunction);

        assertThat(getDiscoveredFunction().getTargetClass()).isEqualTo(myFunction.getClass());
    }

    private void stubCatalogToReturnFunction(String beanName, Function<Object, Object> function) {
        when(catalog.lookupFunction(beanName)).thenReturn(function);
    }

    private void stubCatalogToReturnConsumer(String beanName, Consumer<Object> consumer) {
        when(catalog.lookupConsumer(beanName)).thenReturn(consumer);
    }

    private void stubCatalogToReturnSupplier(String beanName, Supplier<Object> supplier) {
        when(catalog.lookupSupplier(beanName)).thenReturn(supplier);
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


    private SpringCloudMethod getDiscoveredFunction() {
        loader.loadFunction();
        return loader.getFunction();
    }

}
