package com.innowise.orderservice.web.client;

import com.innowise.orderservice.domain.config.FeignClientConfig;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${application.config.user-url}",
        path = "/api/users",
        configuration = FeignClientConfig.class
)
public interface UserClient {
    @GetMapping("/by-uuid/{userId}")
    UserInfoDto getUserById(@PathVariable("userId") UUID id);

}