package com.company.pack

import groovy.util.logging.Slf4j
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Queue
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 *
 */
@Slf4j
class BrokerConfigure {

    public static void main(String... args) throws Exception {
        ApplicationContext context = new AnnotationConfigApplicationContext(RabbitConfiguration.class);
        AmqpAdmin amqpAdmin = context.getBean(AmqpAdmin.class);
        Queue prefetchQueue = (Queue)context.getBean("prefetchQueue")

        log.info "create queue:$prefetchQueue"
        amqpAdmin.declareQueue(prefetchQueue);
    }
}
