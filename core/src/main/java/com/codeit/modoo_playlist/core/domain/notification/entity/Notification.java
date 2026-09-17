package com.codeit.modoo_playlist.core.domain.notification.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 알림 엔티티. createdAt만 필요하고 수정은 없으므로 BaseEntity(생성일시)만 상속한다.
 *
 * 실제 DB 컬럼명은 `type`인데, Swagger/프론트 계약(NotificationDto.level)에 맞추려고
 * 자바 필드명은 level로 유지하고 @Column(name="type")으로만 매핑함.
 *
 * source_id: 이 알림이 어떤 대상에 관한 건지(팔로우한 사람, 플레이리스트 등) 참조용 ID.
 * 현재 Swagger 응답(NotificationDto)엔 노출 안 되고 DB에만 저장 — 나중에 "알림 클릭 시 이동" 같은
 * 기능에 쓸 걸 대비해 DB NOT NULL 제약에 맞춰 채워만 넣는 중.
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "receiver_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID receiverId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationLevel level;

    @Column(name = "source_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sourceId;

}