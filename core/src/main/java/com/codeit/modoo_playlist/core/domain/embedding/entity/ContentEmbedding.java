package com.codeit.modoo_playlist.core.domain.embedding.entity;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "content_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ContentEmbedding extends BaseCreatedEntity {

	@Id
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "content_id", nullable = false)
	private Content contents;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "vector", nullable = false)
	private Map<String, Object> vector;

	@Column(name = "dims", nullable = false)
	private Integer dims;

	@Column(name = "model", nullable = false, length = 50)
	private String model;

}
