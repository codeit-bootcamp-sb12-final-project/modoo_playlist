package com.codeit.modoo_playlist.infra.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

//@Profile("prod")
@Configuration
public class RedisConfig {

  // 키는 문자열로, 값은 JSON으로 직렬화하는 RedisTemplate을 등록
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory connectionFactory,
      JsonMapper objectMapper
  ) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    GenericJacksonJsonRedisSerializer json =
        new GenericJacksonJsonRedisSerializer(redisObjectMapper(objectMapper));

    template.setValueSerializer(json);
    template.setHashValueSerializer(json);

    template.afterPropertiesSet();
    return template;
  }

  private JsonMapper redisObjectMapper(JsonMapper objectMapper) {
    // 공통 매퍼는 유지하고 Redis 전용 매퍼에 객체 복원용 타입 정보를 설정한다.
    return objectMapper.rebuild()
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.codeit.modoo_playlist.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build(),
            DefaultTyping.NON_FINAL_AND_RECORDS,
            JsonTypeInfo.As.PROPERTY
        )
        .build();
  }

}
