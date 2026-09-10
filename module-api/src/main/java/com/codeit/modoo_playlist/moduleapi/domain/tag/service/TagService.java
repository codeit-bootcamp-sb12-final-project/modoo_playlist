package com.codeit.modoo_playlist.moduleapi.domain.tag.service;

import java.util.List;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;

public interface TagService {

    List<Tag> getOrCreateTags(List<String> tagNames);
}
