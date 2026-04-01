package com.innowise.orderservice.kafka;

import com.innowise.orderservice.web.dto.event.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.producer}")
    private String createOrderTopic;

    public void sendOrderCreatedEvent(OrderEventDto eventDto) {
        log.info("Sending order created event to Kafka: {}", eventDto);
        kafkaTemplate.send(createOrderTopic, String.valueOf(eventDto.orderId()), eventDto);
    }
}
