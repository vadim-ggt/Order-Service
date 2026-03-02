package com.innowise.orderservice.web.client;

import com.innowise.orderservice.domain.config.FeignClientConfig;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "${application.config.user-url}",
        path = "/api/users",
        configuration = FeignClientConfig.class
)
public interface UserClient {


    @GetMapping("/by-email")
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    UserInfoDto getUserByEmail(@RequestParam("email") String email);


    default UserInfoDto getUserByEmailFallback(String email, Throwable exception) {
        System.err.println("User Service is down! Fallback activated for email: " +
                email + ". Reason: " +
                exception.getMessage());
        return new UserInfoDto(null, "User data temporarily unavailable",
                "", email, null);
    }
}