package com.codeit.modoo_playlist.core.domain.interaction.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionSource;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
	name = "user_content_interactions",
	uniqueConstraints = @UniqueConstraint(
		name = "UK_INTERACTION",
		columnNames = {"user_id", "content_id", "type"}
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class UserContentInteraction extends BaseUpdatableEntity
{
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "content_id", nullable = false)
	private Content content;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 30)
	private InteractionType type;

	@Column(name = "value", precision = 8, scale = 2)
	private BigDecimal value;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", length = 30)
	private InteractionSource source;

	@ColumnDefault("1")
	@Column(name = "occurrence_count", nullable = false)
	@Builder.Default
	private Integer occurrenceCount = 1;

}
