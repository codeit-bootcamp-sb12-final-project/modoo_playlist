package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.core.global.common.domain.user.entity.User;
//import com.codeit.modoo_playlist.moduleapi.exception.user.UserNotFoundException;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsService implements
    org.springframework.security.core.userdetails.UserDetailsService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  @Override
  public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
//        .orElseThrow(() -> UserNotFoundException.withUsername(username));
        .orElseThrow(() -> new UsernameNotFoundException(username));
    UserDto userDto = userMapper.toDto(user);

    return new UserDetails(
        userDto,
        user.getPassword()
    );
  }
}
