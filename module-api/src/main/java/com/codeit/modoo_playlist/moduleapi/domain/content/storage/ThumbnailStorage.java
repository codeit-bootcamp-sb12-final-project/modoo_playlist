package com.codeit.modoo_playlist.moduleapi.domain.content.storage;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface ThumbnailStorage {

    String store(MultipartFile thumbnail) throws IOException;
}
