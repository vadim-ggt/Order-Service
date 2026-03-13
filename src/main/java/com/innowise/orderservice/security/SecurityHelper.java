package com.innowise.orderservice.security;

import com.innowise.orderservice.domain.dao.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("securityHelper")
@RequiredArgsConstructor
public class SecurityHelper {

    private final OrderRepository orderRepository;

    public boolean isOrderOwner(Long orderId) {
        UUID userIdFromToken = getUserIdFromToken();
        if (userIdFromToken == null) {
            return false;
        }

        return orderRepository.findById(orderId)
                .map(order -> order.getUserId().equals(userIdFromToken))
                .orElse(false);
    }

    private UUID getUserIdFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken token) {
            String sub = token.getToken().getSubject();
            if (sub != null) {
                return UUID.fromString(sub);
            }
        }
        return null;
    }
}
