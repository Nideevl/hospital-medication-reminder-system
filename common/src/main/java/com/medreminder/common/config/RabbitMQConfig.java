package com.medreminder.common.config;

import com.medreminder.common.util.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange medReminderExchange() {
        return new DirectExchange(Constants.RABBITMQ_EXCHANGE_DEFAULT, true, false);
    }

    @Bean
    public Queue escalationQueue() {
        return new Queue(Constants.RABBITMQ_QUEUE_ESCALATION, true);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(Constants.RABBITMQ_QUEUE_EMAIL, true);
    }

    @Bean
    public Queue smsQueue() {
        return new Queue(Constants.RABBITMQ_QUEUE_SMS, true);
    }

    @Bean
    public Binding escalationBinding() {
        return BindingBuilder
                .bind(escalationQueue())
                .to(medReminderExchange())
                .with(Constants.RABBITMQ_ROUTING_KEY_ESCALATION);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(medReminderExchange())
                .with(Constants.RABBITMQ_ROUTING_KEY_EMAIL);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder
                .bind(smsQueue())
                .to(medReminderExchange())
                .with(Constants.RABBITMQ_ROUTING_KEY_SMS);
    }
}