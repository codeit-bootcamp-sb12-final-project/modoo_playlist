package com.codeit.modoo_playlist.moduleapi.config;

import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import redis.clients.jedis.RedisClient;

@Configuration
public class ChatClientConfig {

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
  public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
    return builder
        .defaultSystem(systemPromptResource)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

}
