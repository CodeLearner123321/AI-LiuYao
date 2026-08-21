package com.divination.liuyao.mcp.protocol;

import java.util.List;

public final class McpProtocolSupport {

    public static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-11-25";
    public static final String SERVER_NAME = "ai-liuyao-mcp";
    public static final String SERVER_VERSION = "0.2.0";

    public static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(
        "2025-11-25",
        "2025-06-18",
        "2025-03-26",
        "2024-11-05",
        "2024-10-07"
    );

    private McpProtocolSupport() {
    }

    public static boolean isSupported(String version) {
        return version != null && SUPPORTED_PROTOCOL_VERSIONS.contains(version);
    }
}
