package com.elysion.user.config;

import io.github.bucket4j.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimiterConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private static final Bandwidth LOGIN_BANDWIDTH = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(15))
            .build();
    private static final Bandwidth IP_BLOCK_BANDWIDTH = Bandwidth.builder()
            .capacity(10)
            .refillGreedy(10, Duration.ofHours(1))
            .build();

    @Bean
    public OncePerRequestFilter loginRateLimitingFilter() {
        return new OncePerRequestFilter() {
            private Bucket resolveBucket(String key, Bandwidth limit) {
                return cache.computeIfAbsent(key + "-" + limit.getCapacity(),
                        k -> Bucket.builder().addLimit(limit).build());
            }

            @Override
            protected void doFilterInternal(
                    @NotNull HttpServletRequest request,
                    @NotNull HttpServletResponse response,
                    @NotNull FilterChain filterChain)
                    throws ServletException, IOException {

                if ("/auth/login".equals(request.getServletPath())
                        && "POST".equalsIgnoreCase(request.getMethod())) {

                    String ip = request.getRemoteAddr();
                    Bucket blockBucket   = resolveBucket(ip, IP_BLOCK_BANDWIDTH);
                    Bucket loginBucket   = resolveBucket(ip, LOGIN_BANDWIDTH);

                    if (!blockBucket.tryConsume(1)) {
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.getWriter().write("IP blocked due to suspicious activity.");
                        return;
                    }

                    if (!loginBucket.tryConsume(1)) {
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.getWriter().write("Max 5 login attempts reached. Try later.");
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}
