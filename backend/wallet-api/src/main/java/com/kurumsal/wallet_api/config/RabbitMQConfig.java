package com.kurumsal.wallet_api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_EXCHANGE    = "transaction.exchange";
    public static final String TRANSACTION_QUEUE       = "transaction.queue";
    public static final String TRANSACTION_DLQ         = "transaction.dlq";
    public static final String TRANSACTION_ROUTING_KEY = "transaction.process";
    public static final String TRANSACTION_DL_KEY      = "transaction.dead";

    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue transactionQueue() {
        return QueueBuilder.durable(TRANSACTION_QUEUE)
                .withArgument("x-dead-letter-exchange", TRANSACTION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRANSACTION_DL_KEY)
                .build();
    }

    @Bean
    public Queue transactionDlq() {
        return QueueBuilder.durable(TRANSACTION_DLQ).build();
    }

    @Bean
    public Binding transactionBinding(Queue transactionQueue, DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue).to(transactionExchange).with(TRANSACTION_ROUTING_KEY);
    }

    @Bean
    public Binding transactionDlqBinding(Queue transactionDlq, DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionDlq).to(transactionExchange).with(TRANSACTION_DL_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
