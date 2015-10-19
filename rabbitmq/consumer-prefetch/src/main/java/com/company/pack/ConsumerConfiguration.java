package com.company.pack;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;

/**
 *
 */
public class ConsumerConfiguration extends RabbitConfiguration {

    @Bean
    public SimpleMessageListenerContainer listenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(this.prefetchQueueName);
        container.setConcurrentConsumers(10);
        container.setPrefetchCount(1);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(new MessageListenerAdapter(new MessageHandler()));
        return container;
    }

}
