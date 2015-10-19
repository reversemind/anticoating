package com.company.pack

import groovy.util.logging.Slf4j
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 *
 */
@Slf4j
class QueueProcess {

    public static void main(String... args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(RabbitConfiguration.class);
        AmqpTemplate amqpTemplate = context.getBean(AmqpTemplate.class);
        log.info("Received: " + amqpTemplate.receiveAndConvert().toString());
    }
}
