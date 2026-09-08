package com.codeit.modoo_playlist.core.global.common.entity.baseentity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@ToString
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
public abstract class BaseEntity extends BaseCreatedEntity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	@Column(updatable = false, nullable = false, columnDefinition = "BINARY(16)")
	private UUID id;

}
