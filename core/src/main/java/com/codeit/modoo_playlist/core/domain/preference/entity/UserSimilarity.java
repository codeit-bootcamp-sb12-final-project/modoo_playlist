package com.codeit.modoo_playlist.core.domain.preference.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.codeit.modoo_playlist.core.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_similarities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserSimilarity {

	@EmbeddedId
	private UserSimilarityId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("otherUserId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "other_user_id", nullable = false)
	private User otherUser;

	@Column(name = "score", nullable = false, precision = 6, scale = 4)
	private BigDecimal score;

	@Column(name = "shared_tags", length = 255)
	private String sharedTags;

	@Column(name = "computed_at", nullable = false)
	private Instant computedAt;
}
