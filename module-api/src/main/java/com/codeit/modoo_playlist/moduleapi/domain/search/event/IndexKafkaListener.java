package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaEvent;
import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaPublisher;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexKafkaListener {

  private final ContentIndexService contentIndexService;

  @KafkaListener(
      topics = IndexKafkaPublisher.TOPIC,
      groupId = "content-index",
      properties = "auto.offset.reset=earliest"
  )
  public void handle(IndexKafkaEvent event) {
    log.info("콘텐츠 검색 색인 Kafka 이벤트 수신: contentId={}", event.contentId());
    contentIndexService.index(event.contentId());
  }
}
