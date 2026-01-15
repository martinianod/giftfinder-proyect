package com.findoraai.giftfinder.profile.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple rate limiter for public endpoints
 * Allows 30 requests per minute per IP address
 */
@Component
public class PublicEndpointRateLimiter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute
    
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only apply rate limiting to public endpoints
        if (path.startsWith("/public/")) {
            String clientKey = getClientKey(request);
            
            if (!allowRequest(clientKey)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean allowRequest(String clientKey) {
        long now = Instant.now().toEpochMilli();
        
        requestCounts.compute(clientKey, (key, info) -> {
            if (info == null || now - info.windowStart > WINDOW_SIZE_MS) {
                // New window
                return new RateLimitInfo(now, 1);
            } else {
                // Within current window
                info.count++;
                return info;
            }
        });
        
        RateLimitInfo info = requestCounts.get(clientKey);
        return info.count <= MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientKey(HttpServletRequest request) {
        // Check for real IP behind proxy
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitInfo {
        long windowStart;
        int count;

        RateLimitInfo(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
