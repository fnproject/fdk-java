package com.fnproject.fn.integration;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    int value();

    @Retention(RetentionPolicy.RUNTIME)
    @interface Expected {
        Expect[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Expected.class)
    @interface Expect {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Catch {
        Class<?>[] value();
    }
}
