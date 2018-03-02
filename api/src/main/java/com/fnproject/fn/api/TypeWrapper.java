package com.fnproject.fn.api;

/**
 * Interface representing type information about a possibly-generic type.
 */
public interface TypeWrapper {

    /**
     * Unlike {@link java.lang.reflect.Method}, types (such as parameter types, or return types) are
     * resolved to a reified type even if they are generic, providing reification is possible.
     *
     * For example, take the following classes:
     * <pre>{@code
     * class GenericParent<T> {
     *   public void someMethod(T t) { // do something with t }
     * }
     *
     * class ConcreteClass extends GenericParent<String> { }
     * }</pre>
     *
     * A {@link TypeWrapper} representing the first argument of {@code someMethod} would return {@code String.class}
     * instead of {@code Object.class}
     *
     * @return Reified type
     */
    Class<?> getParameterClass();
}
