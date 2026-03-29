package com.innowise.orderservice.web.client.provider;

import com.innowise.orderservice.domain.exception.ExternalServiceUnavailableException;
import com.innowise.orderservice.web.client.UserClient;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvider {
    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserInfoDto getUserInfoByIdForRead(UUID userId) {
        return userClient.getUserById(userId);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdStrictFallback")
    public UserInfoDto getStrictUserInfoById(UUID userId) {
        return userClient.getUserById(userId);
    }

    private UserInfoDto getUserByIdFallback(UUID userId, Throwable exception) {
        log.warn("User Service is down. Returning dummy user for ID: {}", userId);
        return new UserInfoDto(userId, "User details temporarily unavailable", "N/A", "N/A", null);
    }

    private UserInfoDto getUserByIdStrictFallback(UUID userId, Throwable exception) {
        log.error("User Service is down. Cannot perform strict operation for ID: {}", userId);
        throw new ExternalServiceUnavailableException("User Service is unavailable. Please try again later.");
    }
}
