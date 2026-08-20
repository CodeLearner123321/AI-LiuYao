package com.divination.liuyao.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomCorsFilterTest {

    @Test
    void shouldRejectUnconfiguredOrigin() throws Exception {
        CustomCorsFilter filter = filterWithOrigins("https://trusted.example");
        MockHttpServletResponse response = execute(filter, "https://untrusted.example");

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldAllowConfiguredOriginWithCredentials() throws Exception {
        CustomCorsFilter filter = filterWithOrigins("https://trusted.example,http://localhost:5173");
        MockHttpServletResponse response = execute(filter, "https://trusted.example");

        assertEquals(200, response.getStatus());
        assertEquals("https://trusted.example", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    }

    private CustomCorsFilter filterWithOrigins(String origins) {
        CustomCorsFilter filter = new CustomCorsFilter();
        ReflectionTestUtils.setField(filter, "allowedOrigins", origins);
        return filter;
    }

    private MockHttpServletResponse execute(CustomCorsFilter filter, String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader("Origin", origin);
        request.setRemoteAddr("192.0.2.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
