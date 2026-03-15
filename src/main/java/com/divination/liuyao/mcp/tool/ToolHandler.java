package com.divination.liuyao.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ToolHandler<I, O> {

    String getName();

    String getDescription();

    Class<I> getInputType();

    Class<O> getOutputType();

    ObjectNode execute(JsonNode arguments);
}
