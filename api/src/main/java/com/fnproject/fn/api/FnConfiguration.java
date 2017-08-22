package com.fnproject.fn.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used in user function classes on one or more configuration
 * methods.
 * <p>
 * If the user function method is static, then any annotated configuration
 * method must also be static. This restriction does not apply if the
 * user function method is non-static.
 * <p>
 * The configuration method will be called immediately after the class is
 * loaded and (in the case where the function method is an instance method)
 * after an object of the function class is instantiated. This is still before
 * any function invocations occur.
 * <p>
 * Configuration methods must have a void return type;  methods may
 * have zero arguments or take a single argument of type {@link RuntimeContext} which it
 * may then modify to configure the runtime behaviour of the function.
 *
 * <table summary="Configuration Options">
 * <tr>
 * <td></td>
 * <th>No configuration</th>
 * <th>Static configuration method</th>
 * <th>Instance configuration method</th>
 * </tr>
 * <tr>
 * <th>Static function method</th>
 * <td>OK</td>
 * <td>OK</td>
 * <td>ERROR</td>
 * </tr>
 * <tr>
 * <th>Instance function method</th>
 * <td>OK</td>
 * <td>OK (can only configure runtime)</td>
 * <td>OK (can configure runtime and function instance)</td>
 * </tr>
 * </table>
 * <p>
 * Function classes may have multiple configuration methods and may inherit from classes with configuration methods.
 * <p>
 * When multiple methods are declared in a class hierarchy, all static configuration methods will be called before
 * instance configuration methods and methods declared in super-classes will be called before those in sub-classes.
 * <p>
 * Multiple configuration methods in the same class are supported but no guarantees are made about the ordering of
 * calls.
 * <p>
 * When configuration methods are overridden, the `@FnConfiguration` annotation must be added to the overridden
 * method in order for it to be called.
 * <p>
 * `@FnConfiguration` annotations on interface methods (including default methods and static methods) will be
 * ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FnConfiguration {
}
