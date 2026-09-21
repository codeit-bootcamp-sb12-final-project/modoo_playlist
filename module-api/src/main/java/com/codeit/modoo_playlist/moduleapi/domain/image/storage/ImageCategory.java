package com.codeit.modoo_playlist.moduleapi.domain.image.storage;

public enum ImageCategory {
  CONTENT_THUMBNAIL("contents/thumbnails"),
  USER_PROFILE("users/profiles");

  private final String directory;

  ImageCategory(String directory) {
    this.directory = directory;
  }

  public String directory() {
    return directory;
  }
}
