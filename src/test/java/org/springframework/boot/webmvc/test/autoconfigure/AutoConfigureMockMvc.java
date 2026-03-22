package org.springframework.boot.webmvc.test.autoconfigure;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcTestConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MockMvcTestConfig.class)
public @interface AutoConfigureMockMvc {
}

