package com.company.pack
import groovy.util.logging.Slf4j
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
/**
 *
 */
@Slf4j
class QueueProducer {

    public static void main(String... args) {

        ApplicationContext context = new AnnotationConfigApplicationContext(RabbitConfiguration.class);
        AmqpTemplate amqpTemplate = context.getBean(AmqpTemplate.class);

        final String message = "simple message";

//        final Connection conn = rabbitTemplate.getConnectionFactory().createConnection()
//        final Channel channel = conn.createChannel(true)
//
//        // declare a direct, durable, non autodelete exchange named 'exchangePrefetch'
//        channel.exchangeDeclare("exchangePrefetch", "topic", true);
//
//        // declare a durable, non exclusive, non autodelete queue named 'prefetch.queue'
//        channel.queueDeclare("prefetch.queue", true, false, false, null);
//
//        // bind 'prefetch.queue' to the 'tasks' exchange with the routing key 'routingKey'
//        channel.queueBind("prefetch.queue", "exchangePrefetch", "routingKey");
//
//        channel.basicPublish("exchangePrefetch", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());

        1.times { it ->
            amqpTemplate.convertAndSend("exchangePrefetch", "routingKey", message + ":" + it);
            log.info "Sent: $message:$it"
            Thread.sleep(100)
        }
    }
}
