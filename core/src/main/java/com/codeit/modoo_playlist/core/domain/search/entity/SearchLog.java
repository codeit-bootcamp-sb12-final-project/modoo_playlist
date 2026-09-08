package com.codeit.modoo_playlist.core.domain.search.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "search_logs")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String keyword;

  @Column(name = "ip_address", nullable = false, length = 45)
  private String ipAddress;

  public static SearchLog create(String keyword, String ipAddress) {
    return SearchLog.builder()
        .keyword(keyword)
        .ipAddress(ipAddress)
        .build();
  }

}
