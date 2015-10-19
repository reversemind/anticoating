package com.company.pack
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.AnnotationConfigApplicationContext
/**
 *
 */
@Slf4j
class Consumer {

    public static void main(String... args) throws Exception {
        new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
    }

}
