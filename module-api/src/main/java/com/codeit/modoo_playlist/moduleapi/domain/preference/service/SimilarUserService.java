package com.codeit.modoo_playlist.moduleapi.domain.preference.service;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

public interface SimilarUserService {

  List<SimilarUserDto> getMySimilarUsers(UUID id, @Min(1) @Max(100) Integer limit);
}
