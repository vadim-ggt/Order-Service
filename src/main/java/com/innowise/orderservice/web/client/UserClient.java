package com.innowise.orderservice.web.client;

import com.innowise.orderservice.domain.config.FeignClientConfig;
import com.innowise.orderservice.web.dto.user.UserInfoDto;
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
    UserInfoDto getUserByEmail(@RequestParam("email") String email);

}