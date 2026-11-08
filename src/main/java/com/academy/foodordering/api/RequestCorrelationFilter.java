package com.academy.foodordering.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Convenience diagnostics only; request IDs are NOT business idempotency keys. */
@Component
public final class RequestCorrelationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String input = request.getHeader("X-Request-ID");
        String requestId = input != null && input.matches("[A-Za-z0-9_-]{1,64}")
                ? input : UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", requestId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            chain.doFilter(request, response);
        }
    }
}
