package com.innowise.orderservice.domain.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.innowise.orderservice.web.client")
public class OpenFeignConfig {
}