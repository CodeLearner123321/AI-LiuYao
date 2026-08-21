package com.divination.liuyao.mcp.exception;

/**
 * JSON-RPC / MCP 协议层异常。
 *
 * <p>用于区分协议错误和工具执行错误：
 * 协议错误返回 JSON-RPC error 对象，工具执行错误返回 result.isError=true。</p>
 */
public class McpProtocolException extends RuntimeException {

    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    private final int code;

    public McpProtocolException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static McpProtocolException invalidRequest(String message) {
        return new McpProtocolException(INVALID_REQUEST, message);
    }

    public static McpProtocolException methodNotFound(String message) {
        return new McpProtocolException(METHOD_NOT_FOUND, message);
    }

    public static McpProtocolException invalidParams(String message) {
        return new McpProtocolException(INVALID_PARAMS, message);
    }

    public static McpProtocolException internalError(String message) {
        return new McpProtocolException(INTERNAL_ERROR, message);
    }
}
