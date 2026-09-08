package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
}
