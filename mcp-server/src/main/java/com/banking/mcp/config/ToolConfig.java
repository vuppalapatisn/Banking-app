package com.banking.mcp.config;

import com.banking.mcp.tools.BankingTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link BankingTools} methods as MCP tool callbacks so the Spring AI
 * MCP server auto-configuration advertises them to connected clients over SSE.
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider bankingToolCallbackProvider(BankingTools bankingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(bankingTools)
                .build();
    }
}
