package com.company.pack;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;

/**
 *
 */
public class MessageHandler implements ChannelAwareMessageListener {

    public static Long counter = 1L;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        System.out.println("\n\n: Received: " + message.toString());
        Thread.sleep(1000);
        if (counter++ % 5 == 0) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            throw new InterruptedException("Just break value at:" + counter);
        } else {
            System.out.println("message.getMessageProperties():" + message.getMessageProperties());
            System.out.println("message.getMessageProperties().getDeliveryTag():" + message.getMessageProperties().getDeliveryTag());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
