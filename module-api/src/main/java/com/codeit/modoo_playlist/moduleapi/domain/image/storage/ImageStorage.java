package com.codeit.modoo_playlist.moduleapi.domain.image.storage;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
  String store(MultipartFile image, ImageCategory category) throws IOException;
  void delete(String imageUrl) throws IOException;
}
