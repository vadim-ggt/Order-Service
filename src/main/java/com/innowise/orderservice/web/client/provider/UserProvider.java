package com.innowise.orderservice.web.client.provider;

import com.innowise.orderservice.domain.exception.ExternalServiceUnavailableException;
import com.innowise.orderservice.web.client.UserClient;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvider {
    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    public UserInfoDto getUserInfoForRead(String email) {
        return userClient.getUserByEmail(email);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailStrictFallback")
    public UserInfoDto getStrictUserInfo(String email) {
        return userClient.getUserByEmail(email);
    }

    private UserInfoDto getUserByEmailFallback(String email, Throwable exception) {
        log.warn("User Service is down. Returning dummy user for email: {}", email);
        return new UserInfoDto(null, "User details temporarily unavailable", "N/A", email, null);
    }

    private UserInfoDto getUserByEmailStrictFallback(String email, Throwable exception) {
        log.error("User Service is down. Cannot perform strict operation for email: {}", email);
        throw new ExternalServiceUnavailableException("User Service is unavailable. Please try again later.");
    }
}
