package com.codeit.modoo_playlist.moduleapi.domain.user.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(length = 255, nullable = false, unique = true)
  String email;

  @Column(length = 50, nullable = false)
  String username;

  // 소셜 로그인 계정은 NULL 허용, 일반 가입 시 notnull로.
  @Setter
  @Column(length = 255, nullable = true)
  String password;

  @Column(name = "profile_image_url", length = 500, nullable = true)
  String profileImageUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 20, nullable = false,
      columnDefinition = "VARCHAR(20) DEFAULT 'USER'")
  UserRole role = UserRole.USER;

  @Column(name = "locked", nullable = false)
  boolean locked;

  @Column(name = "temp_password", length = 255, nullable = true)
  String tempPassword;

  @Column(name = "temp_password_expires_at", nullable = true, columnDefinition = "DATETIME(6)")
  Instant tempPasswordExpiresAt;

  @Column(name = "deleted_at", nullable = true)
  Instant deletedAt;

}
