package com.divination.liuyao.mcp.tool;

import com.divination.liuyao.mcp.exception.McpProtocolException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, ToolHandler<?, ?>> handlers = new LinkedHashMap<>();

    public ToolRegistry(List<ToolHandler<?, ?>> toolHandlers) {
        for (ToolHandler<?, ?> toolHandler : toolHandlers) {
            ToolHandler<?, ?> previous = handlers.put(toolHandler.getName(), toolHandler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate MCP tool name: " + toolHandler.getName());
            }
        }
    }

    public Collection<ToolHandler<?, ?>> getAll() {
        return handlers.values();
    }

    public ToolHandler<?, ?> getRequired(String name) {
        ToolHandler<?, ?> handler = handlers.get(name);
        if (name == null || name.isBlank() || handler == null) {
            throw McpProtocolException.invalidParams("Unsupported tool: " + name);
        }
        return handler;
    }
}
