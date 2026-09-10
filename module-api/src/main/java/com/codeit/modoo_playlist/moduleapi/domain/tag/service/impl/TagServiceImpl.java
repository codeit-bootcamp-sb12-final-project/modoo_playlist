package com.codeit.modoo_playlist.moduleapi.domain.tag.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
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

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public List<Tag> getOrCreateTags(List<String> tagNames) {
        List<String> normalizedNames = normalizeNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return List.of();
        }

        Map<String, Tag> tagsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tagRepository.findAllByNameIn(normalizedNames)
                .forEach(tag -> tagsByName.put(tag.getName(), tag));

        normalizedNames.stream()
                .filter(name -> !tagsByName.containsKey(name))
                .forEach(name -> tagRepository.insertIfAbsent(
                        UUID.randomUUID().toString(),
                        name,
                        TagKind.KEYWORD.name()
                ));

        tagsByName.clear();
        tagRepository.findAllByNamesForUpdate(normalizedNames)
                .forEach(tag -> tagsByName.put(tag.getName(), tag));

        return normalizedNames.stream()
                .map(tagsByName::get)
                .toList();
    }

    private List<String> normalizeNames(List<String> tagNames) {
        if (tagNames == null) {
            return List.of();
        }

        Set<String> normalizedNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tagNames.stream()
                .map(String::trim)
                .forEach(normalizedNames::add);
        return List.copyOf(normalizedNames);
    }
}
