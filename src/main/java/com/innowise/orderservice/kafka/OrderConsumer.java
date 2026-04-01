package com.innowise.orderservice.kafka;

import com.innowise.orderservice.domain.service.OrderService;
import com.innowise.orderservice.web.dto.event.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.consumer}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentCreatedEvent(PaymentEventDto eventDto) {
        log.info("Received PaymentEvent from Kafka: {}", eventDto);
        orderService.handlePaymentEvent(eventDto.orderId(), eventDto.status());
    }
}
