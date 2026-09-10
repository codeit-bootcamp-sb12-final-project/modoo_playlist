package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
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

  //  loadUserByUsername 메서드 명은 그대로 사용.
//  내부는 email 조회로 변경
  @Transactional(readOnly = true)
  @Override
  public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String email)
      throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    UserDto userDto = userMapper.toDto(user);

    return new UserDetails(
        userDto,
        user.getPassword()
    );
  }
}
