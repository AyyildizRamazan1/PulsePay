package com.kurumsal.wallet_api.transaction.event;

import com.kurumsal.wallet_api.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes a {@link TransactionEvent} for async post-processing (fraud scoring, notifications)
 * after a transaction has already been committed. Publish failures are logged, not propagated —
 * a broker hiccup must never fail an already-successful financial transaction back to the caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(TransactionEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TRANSACTION_EXCHANGE,
                    RabbitMQConfig.TRANSACTION_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.error("Failed to publish transaction event txId={}", event.transactionId(), e);
        }
    }
}
