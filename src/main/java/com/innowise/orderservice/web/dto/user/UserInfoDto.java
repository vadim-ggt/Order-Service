package com.innowise.orderservice.web.dto.user;

import java.time.LocalDate;
import java.util.UUID;

public record UserInfoDto(
        UUID userId,
        String name,
        String surname,
        String email,
        LocalDate birthDate
) {}
