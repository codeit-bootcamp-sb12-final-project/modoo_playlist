package com.codeit.modoo_playlist.core.domain.user.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
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

  public static User create(
      String email,
      String username,
      String encodedPassword
  ) {
    User user = new User();
    user.email = email;
    user.username = username;
    user.password = encodedPassword;
    user.role = UserRole.USER;
    user.locked = false;
    return user;
  }

  public static User createOAuth(
      String email,
      String username,
      String profileImageUrl
  ) {
    User user = create(email, username, null);
    user.profileImageUrl = profileImageUrl;
    return user;
  }

  public static User createBot(
      String email,
      String username
  ) {
    User user = new User();
    user.email = email;
    user.username = username;
    user.password = null;
    user.role = UserRole.BOT;
    user.locked = false;
    return user;
  }

  public void updateProfile(String username, String profileImageUrl) {
    this.username = username;

    if (profileImageUrl != null) {
      this.profileImageUrl = profileImageUrl;
    }
  }

  public void synchronizeBot(
      String email,
      String username
  ) {
    if (this.role != UserRole.BOT) {
      throw new IllegalStateException("BOT 계정만 동기화할 수 있습니다.");
    }

    this.email = email;
    this.username = username;
    this.password = null;
    this.locked = false;
  }

  public void changeRole(UserRole role) {
    this.role = role;
  }

  public void changeLocked(boolean locked) {
    this.locked = locked;
  }

  public void issueTemporaryPassword(
      String encodedTemporaryPassword,
      Instant expiresAt
  ) {
    if (encodedTemporaryPassword == null || encodedTemporaryPassword.isBlank()) {
      throw new IllegalArgumentException("임시 비밀번호는 비어 있을 수 없습니다.");
    }

    this.tempPassword = encodedTemporaryPassword;
    this.tempPasswordExpiresAt = Objects.requireNonNull(
        expiresAt,
        "임시 비밀번호 만료 시각은 필수입니다."
    );
  }

  public boolean hasActiveTemporaryPassword(Instant now) {
    Objects.requireNonNull(now, "현재 시각은 필수입니다.");

    return tempPassword != null
        && tempPasswordExpiresAt != null
        && now.isBefore(tempPasswordExpiresAt);
  }

  public void changePassword(String encodedPassword) {
    if (encodedPassword == null || encodedPassword.isBlank()) {
      throw new IllegalArgumentException("비밀번호는 비어 있을 수 없습니다.");
    }

    this.password = encodedPassword;
    clearTemporaryPassword();
  }

  public void clearTemporaryPassword() {
    this.tempPassword = null;
    this.tempPasswordExpiresAt = null;
  }

  public void withdraw(Instant withdrawnAt) {
    if (deletedAt != null) {
      throw new IllegalStateException("이미 탈퇴한 계정입니다.");
    }

    this.deletedAt = Objects.requireNonNull(withdrawnAt, "탈퇴 시각은 필수입니다.");
  }
}
