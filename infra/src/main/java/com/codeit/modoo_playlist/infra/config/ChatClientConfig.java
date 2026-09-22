package com.codeit.modoo_playlist.infra.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import redis.clients.jedis.RedisClient;

@Slf4j
@Configuration
public class ChatClientConfig {

  static final String TOOL_FAILURE_MESSAGE =
      "지금은 정보를 조회할 수 없습니다. 같은 툴을 다시 호출하지 말고, 사용자에게 잠시 후 다시 시도해 달라고 안내하세요.";

  @Value("classpath:/prompts/system.st")
  private Resource systemPromptResource;

  @Value("${spring.data.redis.host}")
  private String redisHost;

  @Value("${spring.data.redis.port}")
  private int redisPort;

  @Bean
  public ChatMemory chatMemory() {
    RedisClient jedisClient = RedisClient.builder().hostAndPort(redisHost, redisPort).build();

    ChatMemoryRepository chatMemoryRepository = RedisChatMemoryRepository.builder()
        .jedisClient(jedisClient)
        .indexName("modoo-chat-index")
        .keyPrefix("modoo-chat:")
        .timeToLive(Duration.ofHours(24))
        .build();

    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository)
        .maxMessages(10)
        .build();
  }

  @Bean
  public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
    return exception -> {
      log.error("챗봇 툴 실행 실패: tool={}", exception.getToolDefinition().name(), exception);
      return TOOL_FAILURE_MESSAGE;
    };
  }

  @Bean
  public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
    return builder
        .defaultSystem(systemPromptResource)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

}
