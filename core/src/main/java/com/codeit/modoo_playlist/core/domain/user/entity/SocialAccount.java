package com.codeit.modoo_playlist.core.domain.user.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_social_accounts_provider_user",
            columnNames = {"provider", "provider_user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {
  //  OAuth로 연동 및 로그인
  //  User로 먼저 회원가입을 한 후, 해당 User 사용자와 소셜 계정을 연동.
  //  소셜 로그인으로 사용자 ID를 찾은 후 연결된 User로 로그인

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", length = 20, nullable = false)
  private Provider provider;

  @Column(name = "provider_user_id", length = 255, nullable = false)
  private String providerUserId;
}
