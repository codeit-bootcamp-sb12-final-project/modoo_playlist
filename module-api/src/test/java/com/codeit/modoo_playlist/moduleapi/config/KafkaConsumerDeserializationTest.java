package com.codeit.modoo_playlist.moduleapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.infra.event.WatcherCountChangedEvent;
import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaEvent;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.SerializationUtils;

/**
 * application.yaml 의 컨슈머 설정으로 실제 이벤트를 역직렬화할 수 있는지 확인한다.
 *
 * <p>신뢰 패키지는 하위 패키지를 포함하지 않아서, 이벤트 클래스를 다른 패키지로 옮기거나 새 패키지를 만들면
 * 설정이 어긋나 컨슈머가 같은 메시지에서 멈추고 에러 로그를 끝없이 쌓는다.
 */
class KafkaConsumerDeserializationTest {

  private static final String PREFIX = "spring.kafka.consumer.properties.";

  private ErrorHandlingDeserializer<Object> deserializer() {
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yaml"));
    Map<String, Object> configs = new HashMap<>();
    yaml.getObject().stringPropertyNames().stream()
        .filter(key -> key.startsWith(PREFIX))
        .forEach(key -> configs.put(key.substring(PREFIX.length()), yaml.getObject().getProperty(key)));
    configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
        configs.get("spring.deserializer.value.delegate.class"));
    ErrorHandlingDeserializer<Object> deserializer = new ErrorHandlingDeserializer<>();
    deserializer.configure(configs, false);
    return deserializer;
  }

  private Headers typeHeader(Class<?> type) {
    Headers headers = new RecordHeaders();
    headers.add("__TypeId__", type.getName().getBytes(StandardCharsets.UTF_8));
    return headers;
  }

  @Test
  void 시청자_수_이벤트를_역직렬화한다() {
    UUID contentId = UUID.randomUUID();
    byte[] body = ("{\"contentId\":\"" + contentId + "\"}").getBytes(StandardCharsets.UTF_8);

    Object value = deserializer().deserialize(
        WatcherCountChangedEvent.TOPIC, typeHeader(WatcherCountChangedEvent.class), body);

    assertThat(value).isEqualTo(new WatcherCountChangedEvent(contentId));
  }

  @Test
  void 색인_이벤트를_역직렬화한다() {
    UUID contentId = UUID.randomUUID();
    byte[] body = ("{\"contentId\":\"" + contentId + "\"}").getBytes(StandardCharsets.UTF_8);

    Object value = deserializer().deserialize("content-index", typeHeader(IndexKafkaEvent.class), body);

    assertThat(value).isEqualTo(new IndexKafkaEvent(contentId));
  }

  @Test
  void 신뢰하지_않는_클래스는_예외를_던지지_않고_실패_헤더를_남긴다() {
    Headers headers = new RecordHeaders();
    headers.add("__TypeId__", "com.example.Unknown".getBytes(StandardCharsets.UTF_8));

    Object value = deserializer().deserialize("t", headers, "{}".getBytes(StandardCharsets.UTF_8));

    assertThat(value).isNull();
    assertThat(headers.lastHeader(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER)).isNotNull();
  }
}
