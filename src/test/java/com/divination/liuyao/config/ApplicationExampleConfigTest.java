package com.divination.liuyao.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationExampleConfigTest {

    @Test
    void shouldExposeDocumentedConfigurationKeys() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "application-example",
                new FileSystemResource("src/main/resources/application-example.yml"));

        assertFalse(sources.isEmpty());
        PropertySource<?> source = sources.get(0);
        assertEquals("your_dashscope_api_key", source.getProperty("ai.dashscope.api.key"));
        assertEquals("", source.getProperty("default.api-key"));
        assertEquals("replace_with_a_random_mcp_api_key", source.getProperty("app.mcp.api-key"));
    }
}
