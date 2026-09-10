package com.codeit.modoo_playlist.core.domain.review.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
	name = "reviews",
	uniqueConstraints = @UniqueConstraint(name = "UK_REVIEWS_CONTENT_AUTHOR", columnNames = {"content_id", "author_id"})
)

public class Review extends BaseUpdatableEntity {

	@Column(name = "content_id", columnDefinition = "BINARY(16)", nullable = false)
	private UUID contentId;

	@Column(name = "author_id", columnDefinition = "BINARY(16)", nullable = false)
	private UUID authorId;

	@Column(name = "text", columnDefinition = "TEXT", nullable = false)
	private String text;

	@Column(name = "rating", precision = 2, scale = 1, nullable = false)
	private BigDecimal rating;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private ReviewStatus status;

	public void update(String text, BigDecimal rating) {
		this.text = text;
		this.rating = rating;
	}

}