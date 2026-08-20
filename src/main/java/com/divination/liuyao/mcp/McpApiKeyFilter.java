package com.divination.liuyao.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class McpApiKeyFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.mcp.api-key:}")
    private String configuredApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return !("/mcp".equals(requestUri) || requestUri.startsWith("/mcp/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "MCP API key is not configured");
            return;
        }

        String authorization = request.getHeader("Authorization");
        String suppliedApiKey = authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : request.getHeader("X-MCP-API-Key");

        if (!constantTimeEquals(configuredApiKey, suppliedApiKey)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid MCP API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
