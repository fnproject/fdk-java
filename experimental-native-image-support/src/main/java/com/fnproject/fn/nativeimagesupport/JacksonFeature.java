/*
 * Copyright (c) 2019, 2020, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fnproject.fn.nativeimagesupport;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.oracle.svm.reflect.hosted.ReflectionFeature;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport;

/**
 * This is a graal-native feature that automatically includes any classes referenced as literals in Jackson Annotations
 * from included classes.
 * <p>
 * This is not likely to be complete and may skip annotations in some cases
 * Known gaps:
 * * Annotations on Jackson type super-classes
 */
public class JacksonFeature implements Feature {

    static {

        System.err.println("FnProject experimental Jackson feature loaded");
        System.err.println("Graal native image support is *experimental* it may not be stable and there may be cases where it does not work as expected");
    }

    private static final String JACKSON_PACKAGE_PREFIX = "com.fasterxml.jackson";


    private static boolean shouldIncludeClass(Class<?> clz) {
        return clz != Void.class
                && !clz.getPackage().getName().startsWith(JACKSON_PACKAGE_PREFIX);
    }

    private static boolean isJacksonAnnotation(Annotation a) {
        return a.annotationType().getAnnotation(JacksonAnnotation.class) != null;
    }

    private static Stream<Class<?>> extractLiteralAnnotationRefs(Annotation a) {
        Class<? extends Annotation> aClass = a.annotationType();
        return Arrays.stream(aClass.getDeclaredMethods())
                .flatMap(m -> {
                    Object val;
                    // get annotation value
                    try {
                        val = m.invoke(a);
                    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                        throw new IllegalStateException("Failed to retrieve annotation value from annotation" + a + " method " + m, e);
                    }
                    // technically annotations can't be null but just in case
                    if (val == null) {
                        return Stream.empty();
                    }

                    if (val.getClass().isAnnotation()) { // annotation param on an annotation - descend
                        return extractLiteralAnnotationRefs((Annotation) val);
                    } else if (val.getClass().isArray()) {
                        Class<?> innerType = val.getClass().getComponentType();
                        if (innerType.isAnnotation()) { // list of annotations - descend
                            return Arrays.stream((Annotation[]) val)
                                    .flatMap(JacksonFeature::extractLiteralAnnotationRefs);
                        }
                        return Arrays.stream((Object[]) val) // add class literals in array ref
                                .filter(arrayVal -> arrayVal instanceof Class).map(arrayVal -> (Class<?>) arrayVal);
                    } else if (val instanceof Class) {
                        return Stream.of((Class<?>) val);
                    }
                    return Stream.empty();


                });
    }

    // VisibleForTesting
    protected static Stream<Class<?>> expandAnnotationReferencesForClass(Class<?> clazz) {
        try {
            return Stream.concat(Stream.concat(
                    Arrays.stream(clazz.getAnnotations()),
                    Arrays.stream(clazz.getDeclaredFields()).flatMap(f -> Arrays.stream(f.getAnnotations()))),
                    Arrays.stream(clazz.getDeclaredMethods()).flatMap(m -> Arrays.stream(m.getAnnotations()))
            )
                    .filter(JacksonFeature::isJacksonAnnotation)
                    .flatMap(JacksonFeature::extractLiteralAnnotationRefs)
                    .filter(JacksonFeature::shouldIncludeClass)
                    .peek((clz) -> System.err.println("Extending search for Jackson-referenced class  " + clz + "  from " + clazz));
        } catch (NoClassDefFoundError expected) {
            System.err.println("WARNING: Found unknown class while expanding " + clazz + ":" + expected);
            return Stream.empty();
        }
    }

    @Override
    public List<Class<? extends Feature>> getRequiredFeatures() {
        List<Class<? extends Feature>> fs = new ArrayList<>();
        fs.add(ReflectionFeature.class);
        return fs;
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {

        ClassLoader cl = access.getApplicationClassLoader();
        RuntimeReflectionSupport rrs = ImageSingletons.lookup(RuntimeReflectionSupport.class);

        access.registerSubtypeReachabilityHandler((acc, sourceClazz) ->
                expandAnnotationReferencesForClass(sourceClazz)
                        .forEach((referencedClazz) -> {
                            System.err.println("adding extra Jackson annotated " + referencedClazz + " from " + sourceClazz);
                            acc.registerAsUsed(referencedClazz);
                            acc.registerAsInHeap(referencedClazz);
                            rrs.register(referencedClazz);
                            rrs.register(referencedClazz.getDeclaredConstructors());
                            rrs.register(referencedClazz.getDeclaredMethods());
                            Arrays.stream(referencedClazz.getDeclaredFields()).forEach(f -> rrs.register(false, false, f));
                        }), Object.class);
    }
}
