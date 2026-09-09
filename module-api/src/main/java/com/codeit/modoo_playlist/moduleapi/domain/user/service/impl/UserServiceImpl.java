package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Transactional
  @Override
  public UserDto create(UserCreateRequest request) {
    throw new UnsupportedOperationException("구현 예정");
  }

  @Transactional(readOnly = true)
  @Override
  public UserDto getUser(UUID userId) {
    throw new UnsupportedOperationException("구현 예정");
  }

  @Transactional
  @Override
  public UserDto updateUser(
      UUID userId,
      UserProfileUpdateRequest request,
      MultipartFile image
  ) {
//    본인 수정 권한 검증.

    User user = userRepository.findById(userId).orElse(null);
//    TODO: UserNotFound 예외 정의 후 수정(orElse -> orElseThrow)

    String imageUrl = null;
    if (image != null && !image.isEmpty()) {
      imageUrl = image.getOriginalFilename();
//      TODO: 이미지 저장 후 URL로 받아오는 메서드로 변경
    }

    user.updateProfile(request.name(), imageUrl);
    return userMapper.toDto(user);
  }
}
