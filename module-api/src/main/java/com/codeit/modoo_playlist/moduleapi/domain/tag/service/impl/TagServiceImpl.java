package com.codeit.modoo_playlist.moduleapi.domain.tag.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.tag.repository.jpa.TagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.tag.service.TagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private static final int MAX_TAG_NAME_LENGTH = 50;

    private final TagRepository tagRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Tag> getOrCreateTags(List<String> tagNames) {
        List<String> normalizedNames = normalizeNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return List.of();
        }

        normalizedNames.forEach(name -> tagRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                name,
                TagKind.KEYWORD.name()
        ));

        Map<String, Tag> tagsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tagRepository.findAllByNameIn(normalizedNames)
                .forEach(tag -> tagsByName.put(tag.getName(), tag));

        if (tagsByName.size() != normalizedNames.size()) {
            throw new IllegalStateException("태그 생성 결과를 조회하지 못했습니다.");
        }

        return normalizedNames.stream()
                .map(tagsByName::get)
                .toList();
    }

    private List<String> normalizeNames(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }

        Set<String> normalizedNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String tagName : tagNames) {
            if (tagName == null || tagName.isBlank()) {
                throw new IllegalArgumentException("태그 이름은 비어 있을 수 없습니다.");
            }
            String normalizedName = tagName.trim();
            if (normalizedName.length() > MAX_TAG_NAME_LENGTH) {
                throw new IllegalArgumentException("태그 이름은 50자 이하여야 합니다.");
            }
            normalizedNames.add(normalizedName);
        }
        return List.copyOf(normalizedNames);
    }
}
