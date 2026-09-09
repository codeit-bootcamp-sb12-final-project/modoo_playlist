package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import java.util.Objects;
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
//    TODO: 유저 동시 저장시 DB에서 블록을 거는데, 반환값이 같은지 확인 필요.
    if (userRepository.existsByEmail(request.email())) {
//      TODO: 이 부분도 base 상속해서 UserException 만들어야 함.
      throw new BaseException(ErrorCode.USER_ALREADY_EXISTS);
    }

    String encodedPassword = passwordEncoder.encode(request.password());

    User user = User.create(
        request.email(),
        request.name(),
        encodedPassword
    );

    User savedUser = userRepository.save(user);

    return userMapper.toDto(savedUser);
  }

  @Transactional(readOnly = true)
  @Override
  public UserDto getUser(UUID userId) {

    User user = userRepository.findById(userId).orElse(null);
    //    TODO: UserNotFound 예외 정의 후 수정(orElse -> orElseThrow)

    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public UserDto updateUser(
      UUID actorId,
      UUID userId,
      UserProfileUpdateRequest request,
      MultipartFile image
  ) {
    validateOwner(actorId, userId);

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

  private void validateOwner(UUID actorId, UUID userId) {
    if (!Objects.equals(actorId, userId)) {
//      TODO: USER_NOT_FOUND가 아니라 ACCESS_DENIED로 바꿔야함.
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
  }
}
