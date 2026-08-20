package com.divination.liuyao.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpApiKeyFilterTest {

    @Test
    void shouldRejectMcpWhenApiKeyIsNotConfigured() throws Exception {
        McpApiKeyFilter filter = filterWithApiKey("");
        MockHttpServletResponse response = execute(filter, null);

        assertEquals(503, response.getStatus());
    }

    @Test
    void shouldRejectInvalidMcpApiKey() throws Exception {
        McpApiKeyFilter filter = filterWithApiKey("expected-key");
        MockHttpServletResponse response = execute(filter, "Bearer wrong-key");

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAllowValidBearerApiKey() throws Exception {
        McpApiKeyFilter filter = filterWithApiKey("expected-key");
        MockHttpServletResponse response = execute(filter, "Bearer expected-key");

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldNotFilterUnrelatedPathWithMcpPrefix() throws Exception {
        McpApiKeyFilter filter = filterWithApiKey("");
        MockHttpServletResponse response = execute(filter, "/mcp-other", null);

        assertEquals(200, response.getStatus());
    }

    private McpApiKeyFilter filterWithApiKey(String apiKey) {
        McpApiKeyFilter filter = new McpApiKeyFilter();
        ReflectionTestUtils.setField(filter, "configuredApiKey", apiKey);
        return filter;
    }

    private MockHttpServletResponse execute(McpApiKeyFilter filter, String authorization) throws Exception {
        return execute(filter, "/mcp", authorization);
    }

    private MockHttpServletResponse execute(McpApiKeyFilter filter, String path, String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
