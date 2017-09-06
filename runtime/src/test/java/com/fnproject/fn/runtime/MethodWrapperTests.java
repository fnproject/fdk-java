package com.fnproject.fn.runtime;

import com.fnproject.fn.api.MethodWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class MethodWrapperTests {
    private final Class srcClass;
    private final String methodName;
    private final int parameterIndex;
    private final Class[] methodParameterTypes;
    private final Class<?> expectedType;

    public MethodWrapperTests(Class srcClass, String methodName, Class[] methodParameterTypes, int parameterIndex, Class<?> expectedType) throws Exception {
        this.srcClass = srcClass;
        this.methodName = methodName;
        this.parameterIndex = parameterIndex;
        this.methodParameterTypes = methodParameterTypes;
        this.expectedType = expectedType;
    }

    @Test
    public void testMethodParameterHasExpectedType() throws NoSuchMethodException {
        MethodWrapper method = new DefaultMethodWrapper(srcClass, srcClass.getMethod(this.methodName, this.methodParameterTypes));

        if (parameterIndex >= 0) {
            assertThat(method.getParamType(parameterIndex).getParameterClass()).isEqualTo(expectedType);
        } else {
            assertThat(parameterIndex).isEqualTo(-1)
                    .withFailMessage("You can only use non negative parameter indices or -1 to represent return value in this test suite");
            assertThat(method.getReturnType().getParameterClass()).isEqualTo(expectedType);
        }
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return Arrays.asList(new Object[][] {
                { ConcreteTypeExamples.class, "voidReturnType", new Class[]{ }, -1, Void.class },
                { ConcreteTypeExamples.class, "singleParameter", new Class[]{ String.class }, 0, String.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ boolean.class}, 0, Boolean.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ byte.class }, 0, Byte.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ char.class }, 0, Character.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ short.class }, 0, Short.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ int.class }, 0, Integer.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ long.class }, 0, Long.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ float.class }, 0, Float.class },
                { ConcreteTypeExamples.class, "singlePrimitiveParameter", new Class[]{ double.class }, 0, Double.class },
                { ConcreteTypeExamples.class, "multipleParameters", new Class[] { String.class, double.class }, 0, String.class },
                { ConcreteTypeExamples.class, "multipleParameters", new Class[]{ String.class, double.class }, 1, Double.class },
                { ConcreteTypeExamples.class, "singleGenericParameter", new Class[]{ List.class }, 0, List.class },
                { ConcreteTypeExamples.class, "noArgs", new Class[]{}, -1, String.class },
                { ConcreteTypeExamples.class, "noArgsWithPrimitiveReturnType", new Class[]{}, -1, Integer.class },
                { ConcreteSubclassOfGenericParent.class, "singleGenericArg", new Class[] { Object.class }, 0, String.class },
                { ConcreteSubclassOfGenericParent.class, "returnsGeneric", new Class[] { }, -1, String.class },
                { ConcreteSubclassOfNestedGenericAncestors.class, "methodWithGenericParamAndReturnType", new Class[]{ Object.class }, 0, Integer.class },
                { ConcreteSubclassOfNestedGenericAncestors.class, "methodWithGenericParamAndReturnType", new Class[]{ Object.class }, -1, String.class }
        });
    }

    static class ConcreteTypeExamples {
        public void voidReturnType() { };
        public void singleParameter(String s) { };
        public void singlePrimitiveParameter(boolean i) { };
        public void singlePrimitiveParameter(byte i) { };
        public void singlePrimitiveParameter(char i) { };
        public void singlePrimitiveParameter(short i) { };
        public void singlePrimitiveParameter(int i) { };
        public void singlePrimitiveParameter(long i) { };
        public void singlePrimitiveParameter(float i) { };
        public void singlePrimitiveParameter(double i) { };
        public void multipleParameters(String s, double i) { };
        public String noArgs() { return ""; };
        public int noArgsWithPrimitiveReturnType() { return 1; };
        public void singleGenericParameter(List<String> s) { };
    }

    static class ParentClassWithGenericType<T> {
        public void singleGenericArg(T t) { };
        public T returnsGeneric() { return null; };
    }

    static class ConcreteSubclassOfGenericParent extends ParentClassWithGenericType<String> { }

    static class GenericParent<T, U> {
        public T methodWithGenericParamAndReturnType(U u) { return null; }
    }
    static class SpecialisedGenericParent<T> extends GenericParent<T, Integer> { }
    static class ConcreteSubclassOfNestedGenericAncestors extends SpecialisedGenericParent<String> { }
}
