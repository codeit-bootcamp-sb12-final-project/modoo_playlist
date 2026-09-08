package com.codeit.modoo_playlist.core.domain.preference.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserSimilarityId implements Serializable {

	@Serial
	private static final long serialVersionUID = 5497399214262758116L;

	@Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(name = "other_user_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID otherUserId;
}
