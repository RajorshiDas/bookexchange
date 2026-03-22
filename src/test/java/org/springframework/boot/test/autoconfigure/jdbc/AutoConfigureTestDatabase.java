package org.springframework.boot.test.autoconfigure.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoConfigureTestDatabase {

    Replace replace() default Replace.ANY;

    enum Replace {
        ANY,
        AUTO_CONFIGURED,
        NONE
    }
}

