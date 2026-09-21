package com.codeit.modoo_playlist.infra.event.kafka;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexKafkaPublisher {

  public static final String TOPIC = "content-index";

  private final KafkaTemplate<String, IndexKafkaEvent> kafkaTemplate;

  public void publish(UUID contentId) {
    IndexKafkaEvent event = new IndexKafkaEvent(contentId);

    kafkaTemplate.send(TOPIC, contentId.toString(), event)
        .whenComplete((result, exception) -> {
          if (exception != null) {
            log.error("콘텐츠 검색 색인 Kafka 이벤트 전송 실패: contentId={}", contentId, exception);
          }
        });
  }
}
