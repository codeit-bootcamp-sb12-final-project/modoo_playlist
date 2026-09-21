package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentPersonRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentSportsRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentDetailServiceImpl implements ContentDetailService {

  private static final int MAX_DESCRIPTION_LENGTH = 300;
  private static final int MAX_ACTORS = 5;
  private static final String DIRECTOR = "DIRECTOR";
  private static final String ACTOR = "ACTOR";

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentPersonRepository contentPersonRepository;
  private final ContentSportsRepository contentSportsRepository;

  @Override
  public List<ContentDetailDto> getDetails(List<UUID> contentIds) {
    if (contentIds.isEmpty()) {
      return List.of();
    }

    Map<UUID, Content> contents = contentRepository.findAllById(contentIds).stream()
        .filter(content -> content.getDeletedAt() == null)
        .collect(Collectors.toMap(Content::getId, Function.identity()));
    Map<UUID, List<String>> tags = contentTagRepository.findAllWithTagByContentIds(contentIds).stream()
        .collect(Collectors.groupingBy(
            contentTag -> contentTag.getId().getContentId(),
            Collectors.mapping(contentTag -> contentTag.getTag().getName(), Collectors.toList())));
    Map<UUID, ContentSports> sports = contentSportsRepository.findAllById(contentIds).stream()
        .collect(Collectors.toMap(ContentSports::getContentId, Function.identity()));

    return contentIds.stream()
        .filter(contents::containsKey)
        .map(id -> toDto(
            contents.get(id),
            tags.getOrDefault(id, List.of()).stream().sorted().toList(),
            contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(id),
            sports.get(id)))
        .toList();
  }

  private static ContentDetailDto toDto(
      Content content, List<String> tags, List<ContentPerson> people, ContentSports sports) {
    return new ContentDetailDto(
        content.getId(),
        content.getTitle(),
        content.getType(),
        content.getReleaseDate() == null ? null : content.getReleaseDate().getYear(),
        content.getOriginCountry(),
        tags,
        namesOf(people, DIRECTOR, Integer.MAX_VALUE),
        namesOf(people, ACTOR, MAX_ACTORS),
        truncate(content.getDescription()),
        sports == null ? null : new ContentDetailDto.SportsInfo(
            sports.getSportType(), sports.getLeague(), sports.getSeason(), sports.getHomeTeam(),
            sports.getAwayTeam(), sports.getVenue(), sports.getStatus(), sports.getKickoffAt()));
  }

  private static List<String> namesOf(List<ContentPerson> people, String roleType, int limit) {
    return people.stream()
        .filter(person -> roleType.equals(person.getRoleType()))
        .map(ContentPerson::getPersonName)
        .limit(limit)
        .toList();
  }

  private static String truncate(String description) {
    if (description == null || description.length() <= MAX_DESCRIPTION_LENGTH) {
      return description;
    }
    return description.substring(0, MAX_DESCRIPTION_LENGTH) + "…";
  }
}
