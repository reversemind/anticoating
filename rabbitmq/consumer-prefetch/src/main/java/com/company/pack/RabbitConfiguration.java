package com.company.pack;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class RabbitConfiguration {

    protected final String prefetchQueueName = "prefetch.queue";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());

        //The routing key is set to the name of the queue by the broker for the default exchange.
        template.setRoutingKey(this.prefetchQueueName);

        // synchronously receive messages from
        template.setQueue(this.prefetchQueueName);
        return template;
    }

    @Bean
    // Every queue is bound to the default direct exchange
    public Queue prefetchQueue() {
        return new Queue(this.prefetchQueueName, true, false, false);
    }

//    @Bean
//    public SimpleMessageListenerContainer messageListenerContainer(){
//        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer(connectionFactory());
//        simpleMessageListenerContainer.setQueueNames(prefetchQueueName);
//
//        // the one
//        simpleMessageListenerContainer.setMaxConcurrentConsumers(1);
//        simpleMessageListenerContainer.setPrefetchCount(1);
//
//        simpleMessageListenerContainer.setMessageListener(messageListener());
//        return simpleMessageListenerContainer;
//    }
//
//    @Bean
//    public Object messageListener(){
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter();
//        messageListenerAdapter.setDelegate(messageHandler());
//        return messageListenerAdapter;
//    }
//
//    @Bean
//    public MessageHandler messageHandler(){
//        return new MessageHandler();
//    }
}
