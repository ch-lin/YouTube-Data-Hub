package ch.lin.youtube.hub.backend.api.mcp;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public List<ToolCallback> temMcpTools(ItemMcpTools itemService) {
        return Arrays.asList(ToolCallbacks.from(itemService));
    }
}
