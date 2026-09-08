package com.codeit.modoo_playlist.core.domain.preference.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "user_preference_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserPreferenceTag {

	@EmbeddedId
	private UserPreferenceTagId id;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("tagId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "tag_id", nullable = false)
	private Tag tag;

	@ColumnDefault("0.0000")
	@Column(name = "score", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal score = BigDecimal.ZERO;

	@ColumnDefault("0.0000")
	@Column(name = "raw_score", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal rawScore = BigDecimal.ZERO;

	@Column(name = "last_signal_at")
	private Instant lastSignalAt;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
