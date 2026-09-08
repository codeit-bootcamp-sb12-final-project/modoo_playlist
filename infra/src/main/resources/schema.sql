SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `user_similarities`;
DROP TABLE IF EXISTS `playlist_subscriptions`;
DROP TABLE IF EXISTS `social_accounts`;
DROP TABLE IF EXISTS `content_videos`;
DROP TABLE IF EXISTS `watching_sessions`;
DROP TABLE IF EXISTS `user_preference_tag`;
DROP TABLE IF EXISTS `content_review_summaries`;
DROP TABLE IF EXISTS `playlist_contents`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `messages`;
DROP TABLE IF EXISTS `content_sports`;
DROP TABLE IF EXISTS `conversations`;
DROP TABLE IF EXISTS `content_embeddings`;
DROP TABLE IF EXISTS `content_tags`;
DROP TABLE IF EXISTS `playlists`;
DROP TABLE IF EXISTS `content_people`;
DROP TABLE IF EXISTS `reviews`;
DROP TABLE IF EXISTS `tags`;
DROP TABLE IF EXISTS `user_content_interactions`;
DROP TABLE IF EXISTS `contents`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `follows`;

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE `users` (
	`id`	BINARY(16)	NOT NULL,
	`email`	VARCHAR(255)	NOT NULL	COMMENT '소셜 생성 계정과 일반 가입이 같은 이메일을 나누어 갖지 못하게 막는 지점',
	`username`	VARCHAR(50)	NOT NULL,
	`password`	VARCHAR(255)	NULL	COMMENT 'NULL = 소셜 전용 계정(비밀번호 로그인/초기화 불가). 일반 가입은 항상 값 존재',
	`profile_image_url`	VARCHAR(500)	NULL,
	`role`	VARCHAR(20)	NOT NULL	DEFAULT 'USER'	COMMENT 'USER / ADMIN',
	`locked`	BOOLEAN	NOT NULL	DEFAULT 0,
	`temp_password`	VARCHAR(255)	NULL,
	`temp_password_expires_at`	DATETIME(6)	NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	`deleted_at`	DATETIME(6)	NULL,

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_USERS_EMAIL` (`email`)
);


CREATE TABLE `contents` (
	`id`	BINARY(16)	NOT NULL,
	`type`	VARCHAR(20)	NOT NULL,
	`title`	VARCHAR(255)	NOT NULL,
	`description`	TEXT	NULL,
	`thumbnail_url`	VARCHAR(500)	NULL,
	`source`	VARCHAR(20)	NULL,
	`source_id`	VARCHAR(100)	NULL,
	`release_date`	DATE	NULL	COMMENT '영화/드라마 개봉일, 스포츠 경기일',
	`origin_country`	VARCHAR(20)	NULL	COMMENT 'KR / US / JP 등 제작 국가. 태그로도 활용',
	`average_rating`	DECIMAL(2,1)	NOT NULL	DEFAULT 0.0,
	`review_count`	INT	NOT NULL	DEFAULT 0,
	`rating_sum`	DECIMAL(10,2)	NOT NULL	DEFAULT 0.00	COMMENT '평균 재계산 없이 원자적 UPDATE 하기 위한 누적합',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	`deleted_at`	DATETIME(6)	NULL	COMMENT 'NULL = 살아있는 콘텐츠. 목록/검색에서만 제외하고 리뷰·시청이력은 보존',
	`embedding_source_hash`	CHAR(64)	NULL,

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_CONTENTS_SOURCE_SOURCE_ID` (`source`, `source_id`),
	KEY `IDX_CONTENTS_LIVE_TYPE` (`deleted_at`, `type`)	COMMENT 'MySQL은 부분 인덱스가 없어 deleted_at을 선행 컬럼으로',
	KEY `IDX_CONTENTS_RELEASE` (`release_date`)
);


CREATE TABLE `content_videos` (
	`content_id`	BINARY(16)	NOT NULL,
	`runtime_minutes`	INT	NULL,
	`collection_name`	VARCHAR(100)	NULL,
	`imdb_id`	VARCHAR(20)	NULL,
	`release_status`	VARCHAR(30)	NULL,
	`number_of_seasons`	INT	NULL,
	`number_of_episodes`	INT	NULL,
	`original_language`	VARCHAR(10)	NULL,
	`popularity`	FLOAT	NULL,
	`external_rating`	DECIMAL(3,1)	NULL,
	`external_rating_count`	INT	NULL,

	PRIMARY KEY (`content_id`),
	KEY `IDX_VIDEO_RUNTIME` (`runtime_minutes`)
);


CREATE TABLE `content_sports` (
	`content_id`	BINARY(16)	NOT NULL	COMMENT 'contents와 1:1. type=SPORT 인 행에만 존재',
	`league`	VARCHAR(100)	NULL	COMMENT 'strLeague. 필터에 쓰려면 tags(kind=GENRE)에도 넣을 것',
	`season`	VARCHAR(20)	NULL	COMMENT 'strSeason (예: 2025-2026)',
	`home_team`	VARCHAR(100)	NULL,
	`away_team`	VARCHAR(100)	NULL,
	`home_score`	INT	NULL	COMMENT '경기 전이면 NULL',
	`away_score`	INT	NULL	COMMENT '경기 전이면 NULL',
	`venue`	VARCHAR(100)	NULL,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'SCHEDULED'	COMMENT 'SCHEDULED / LIVE / FINISHED. LIVE여야 같이보기가 의미가 있다',
	`kickoff_at`	DATETIME(6)	NULL	COMMENT 'dateEvent + strTime. contents.release_date는 날짜만 담는다',

	PRIMARY KEY (`content_id`),
	KEY `IDX_SPORTS_KICKOFF` (`status`, `kickoff_at`)	COMMENT '오늘 경기 / 곧 시작 질의용'
);


CREATE TABLE `content_people` (
	`id`	BINARY(16)	NOT NULL,
	`content_id`	BINARY(16)	NOT NULL,
	`role_type`	VARCHAR(20)	NOT NULL,
	`person_name`	VARCHAR(100)	NOT NULL,
	`character_name`	VARCHAR(100)	NULL,
	`display_order`	INT	NOT NULL	DEFAULT 0,
	`person_id`	VARCHAR(100)	NULL,
	`person_img`	TEXT	NULL,

	PRIMARY KEY (`id`),
	KEY `IDX_PEOPLE_CONTENT` (`content_id`, `display_order`),
	KEY `IDX_PEOPLE_NAME` (`person_name`)	COMMENT '인물 기반 탐색'
);


CREATE TABLE `content_embeddings` (
	`content_id`	BINARY(16)	NOT NULL,
	`vector`	JSON	NOT NULL,
	`dims`	INT	NOT NULL,
	`model`	VARCHAR(50)	NOT NULL	COMMENT '모델 교체 시 벡터 공간이 달라지므로 전체 재생성 판단 근거',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`content_id`),
	KEY `IDX_EMBEDDING_MODEL` (`model`)
);


CREATE TABLE `tags` (
	`id`	BINARY(16)	NOT NULL,
	`name`	VARCHAR(50)	NOT NULL	COMMENT 'LLM 표기 흔들림(SF/공상과학/sci-fi)을 정규화하는 지점',
	`kind`	VARCHAR(20)	NOT NULL	COMMENT 'GENRE / THEME / MOOD. 추천 점수 계수 분기',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_TAGS_NAME` (`name`)
);


CREATE TABLE `content_tags` (
	`tag_id`	BINARY(16)	NOT NULL,
	`content_id`	BINARY(16)	NOT NULL,
	`source`	VARCHAR(20)	NULL	COMMENT 'OPENAPI / LLM. 같은 태그가 A엔 TMDB로 B엔 LLM으로 붙을 수 있어 쌍의 속성',

	PRIMARY KEY (`tag_id`, `content_id`),	-- 선행 tag_id: 취향 태그로 콘텐츠 후보를 뽑는 방향이 주 질의
	KEY `IDX_CONTENT_TAGS_REVERSE` (`content_id`)	COMMENT '콘텐츠의 태그 목록 조회 (역방향)'
);


CREATE TABLE `reviews` (
	`id`	BINARY(16)	NOT NULL,
	`content_id`	BINARY(16)	NOT NULL,
	`author_id`	BINARY(16)	NOT NULL,
	`text`	TEXT	NOT NULL,
	`rating`	DECIMAL(2,1)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
	`status`	VARCHAR(20)	NOT NULL,

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_REVIEWS_CONTENT_AUTHOR` (`content_id`, `author_id`)	COMMENT '평균 평점 왜곡 방지',
	KEY `IDX_REVIEWS_AUTHOR` (`author_id`, `created_at` DESC)	COMMENT '내가 쓴 리뷰 목록',
	KEY `IDX_REVIEWS_CONTENT` (`content_id`, `created_at` DESC)
);


CREATE TABLE `content_review_summaries` (
	`content_id`	BINARY(16)	NOT NULL,
	`summary`	TEXT	NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`content_id`)
);


CREATE TABLE `playlists` (
	`id`	BINARY(16)	NOT NULL,
	`owner_id`	BINARY(16)	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`description`	VARCHAR(500)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)	COMMENT '플레이리스트 목록 정렬 기준(sortBy=updatedAt)',
	`generated_by`	VARCHAR(20)	NOT NULL	DEFAULT 'USER'	COMMENT 'USER / AI',

	PRIMARY KEY (`id`),
	KEY `IDX_PLAYLISTS_OWNER` (`owner_id`, `updated_at` DESC)
);


CREATE TABLE `playlist_contents` (
	`playlist_id`	BINARY(16)	NOT NULL,
	`content_id`	BINARY(16)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`playlist_id`, `content_id`),
	KEY `IDX_PLAYLIST_CONTENTS_CONTENT` (`content_id`)	COMMENT '이 콘텐츠가 담긴 플레이리스트 역조회'
);


CREATE TABLE `playlist_subscriptions` (
	`playlist_id`	BINARY(16)	NOT NULL,
	`subscriber_id`	BINARY(16)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`playlist_id`, `subscriber_id`),
	KEY `IDX_SUBSCRIPTIONS_SUBSCRIBER` (`subscriber_id`)	COMMENT '내가 구독한 플레이리스트 목록'
);


CREATE TABLE `follows` (
	`id`	BINARY(16)	NOT NULL,
	`follower_id`	BINARY(16)	NOT NULL,
	`followee_id`	BINARY(16)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_FOLLOWS_PAIR` (`follower_id`, `followee_id`)	COMMENT '중복 팔로우 방지. 자기 자신 팔로우는 앱에서 차단',
	KEY `IDX_FOLLOWS_FOLLOWEE` (`followee_id`)
);


CREATE TABLE `social_accounts` (
	`id`	BINARY(16)	NOT NULL,
	`user_id`	BINARY(16)	NOT NULL,
	`provider`	VARCHAR(20)	NOT NULL,
	`provider_user_id`	VARCHAR(255)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_SOCIAL_PROVIDER_USER` (`provider`, `provider_user_id`),
	KEY `IDX_SOCIAL_USER` (`user_id`)
);


CREATE TABLE `watching_sessions` (
	`id`	BINARY(16)	NOT NULL,
	`watcher_id`	BINARY(16)	NOT NULL	COMMENT '사용자당 ended_at IS NULL 행은 1개만. MySQL에 부분 UNIQUE가 없어 앱에서 보장(JOIN 시 이전 세션 종료)',
	`content_id`	BINARY(16)	NOT NULL,
	`started_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`ended_at`	DATETIME(6)	NULL	COMMENT 'NULL = 지금 시청 중. LEAVE 시 채워넣고 행은 유지(삭제 금지)',
	`watched_seconds`	INT	NULL	COMMENT '세션 참여 시간(JOIN~LEAVE). 영상 재생 시간이 아님 — 스트리밍 기능 없음',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	KEY `IDX_WATCHING_ACTIVE` (`content_id`, `ended_at`)	COMMENT '지금 이 콘텐츠 보는 사람 조회',
	KEY `IDX_WATCHING_USER` (`watcher_id`, `started_at` DESC)
);


CREATE TABLE `user_content_interactions` (
	`id`	BINARY(16)	NOT NULL,
	`user_id`	BINARY(16)	NOT NULL,
	`content_id`	BINARY(16)	NOT NULL,
	`type`	VARCHAR(30)	NOT NULL	COMMENT 'LIKE / DISLIKE / NOT_INTERESTED / VIEW / PLAYLIST_ADD / REVIEW_WRITE / WATCH_SESSION / TRAILER_WATCH / MARK_WATCHED',
	`value`	DECIMAL(8,2)	NULL	COMMENT '타입별 수치 — 체류초, 세션초, 예고편 재생률(0~1), 평점',
	`source`	VARCHAR(30)	NULL	COMMENT 'HOME_ROW / SEARCH / CHAT / PLAYLIST / PROFILE / DIRECT. 유입 경로별 성과 비교',
	`occurrence_count`	INT	NOT NULL	DEFAULT 1	COMMENT 'VIEW처럼 반복되는 타입의 누적 횟수',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	UNIQUE KEY `UK_INTERACTION` (`user_id`, `content_id`, `type`),
	KEY `IDX_INTERACTION_USER_TIME` (`user_id`, `created_at` DESC)	COMMENT '취향 벡터 계산 배치의 주 질의',
	KEY `IDX_INTERACTION_CONTENT` (`content_id`, `type`),
	KEY `IDX_INTERACTION_SOURCE` (`source`, `created_at`)
);


CREATE TABLE `user_preference_tag` (
	`user_id`	BINARY(16)	NOT NULL,
	`tag_id`	BINARY(16)	NOT NULL,
	`score`	DECIMAL(10,4)	NOT NULL	DEFAULT 0.0000	COMMENT '시간감쇠·IDF 반영 최종 점수',
	`raw_score`	DECIMAL(10,4)	NOT NULL	DEFAULT 0.0000	COMMENT '감쇠 전 누적값. 파라미터 변경 시 재계산용',
	`last_signal_at`	DATETIME(6)	NULL	COMMENT '마지막 신호 시각. 배치 재계산 시 갱신하지 말 것',
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`user_id`, `tag_id`),	-- 선행 user_id: 조회가 항상 사용자 기준
	KEY `IDX_PREF_USER_SCORE` (`user_id`, `score` DESC)	COMMENT '내 상위 태그 N개 — 최빈 질의',
	KEY `IDX_PREF_TAG_SCORE` (`tag_id`, `score` DESC)	COMMENT '유사 사용자 후보 축소 시 태그 역인덱스'
);


CREATE TABLE `user_similarities` (
	`user_id`	BINARY(16)	NOT NULL,
	`other_user_id`	BINARY(16)	NOT NULL,
	`score`	DECIMAL(6,4)	NOT NULL,
	`shared_tags`	VARCHAR(255)	NULL	COMMENT '겹치는 상위 태그명. 근거 문구 생성용 (콘텐츠는 노출 금지 — 역추론 방지)',
	`computed_at`	DATETIME(6)	NOT NULL,

	PRIMARY KEY (`user_id`, `other_user_id`),	-- 선행 user_id: 조회가 항상 사용자 기준
	KEY `IDX_SIMILARITY_SCORE` (`user_id`, `score` DESC)
);


CREATE TABLE `conversations` (
	`id`	BINARY(16)	NOT NULL,
	`participant_id`	BINARY(16)	NOT NULL,
	`type`	VARCHAR(20)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`updated_at`	DATETIME(6)	NULL	DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`)
);


CREATE TABLE `messages` (
	`id`	BINARY(16)	NOT NULL,
	`user1_id`	BINARY(16)	NOT NULL,
	`user2_id`	BINARY(16)	NULL,
	`content_id`	BINARY(16)	NULL,
	`conversation_id`	BINARY(16)	NOT NULL,
	`type`	VARCHAR(20)	NOT NULL,
	`content`	TEXT	NOT NULL,
	`read_at`	DATETIME(6)	NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),

	PRIMARY KEY (`id`),
	KEY `IDX_MESSAGES_CONVERSATION` (`conversation_id`, `created_at` DESC),
	KEY `IDX_MESSAGES_RECEIVER` (`user2_id`, `read_at`),
	KEY `IDX_MESSAGES_CONTENT` (`content_id`)
);


CREATE TABLE `notifications` (
	`id`	BINARY(16)	NOT NULL,
	`receiver_id`	BINARY(16)	NOT NULL,
	`title`	VARCHAR(255)	NOT NULL,
	`content`	VARCHAR(500)	NOT NULL,
	`created_at`	DATETIME(6)	NOT NULL	DEFAULT CURRENT_TIMESTAMP(6),
	`type`	VARCHAR(20)	NOT NULL,
	`source_id`	BINARY(16)	NOT NULL,
	`is_read`	BOOLEAN	NOT NULL	DEFAULT 0,

	PRIMARY KEY (`id`),
	KEY `IDX_NOTIFICATIONS_RECEIVER` (`receiver_id`, `created_at` DESC)	COMMENT 'SSE 재연결 시 Last-Event-ID 이후 조회에도 사용'
);


-- =====================================================================
-- Foreign Key
--
-- ON UPDATE RESTRICT : PK가 UUID라 갱신될 일이 없다. 갱신 시도를 막는다
-- ON DELETE CASCADE  : 부모가 사라지면 존재 의미가 없는 종속 데이터
-- ON DELETE RESTRICT : 이력·집계 근거라 남아야 하는 데이터
--                      users / contents는 소프트 딜리트이므로 물리 삭제 차단
-- =====================================================================

-- 콘텐츠 종속 (1:1, 부속 정보)
ALTER TABLE `content_videos` ADD CONSTRAINT `FK_contents_TO_content_videos_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_sports` ADD CONSTRAINT `FK_contents_TO_content_sports_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_people` ADD CONSTRAINT `FK_contents_TO_content_people_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_embeddings` ADD CONSTRAINT `FK_contents_TO_content_embeddings_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_review_summaries` ADD CONSTRAINT `FK_contents_TO_content_review_summaries_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_tags` ADD CONSTRAINT `FK_tags_TO_content_tags_1`
	FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `content_tags` ADD CONSTRAINT `FK_contents_TO_content_tags_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

-- 사용자 종속 (계정이 사라지면 함께 정리)
ALTER TABLE `social_accounts` ADD CONSTRAINT `FK_users_TO_social_accounts_1`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `follows` ADD CONSTRAINT `FK_users_TO_follows_1`
	FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `follows` ADD CONSTRAINT `FK_users_TO_follows_2`
	FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `notifications` ADD CONSTRAINT `FK_users_TO_notifications_1`
	FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `playlists` ADD CONSTRAINT `FK_users_TO_playlists_1`
	FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

-- 플레이리스트 종속
ALTER TABLE `playlist_contents` ADD CONSTRAINT `FK_playlists_TO_playlist_contents_1`
	FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `playlist_contents` ADD CONSTRAINT `FK_contents_TO_playlist_contents_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `playlist_subscriptions` ADD CONSTRAINT `FK_playlists_TO_playlist_subscriptions_1`
	FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `playlist_subscriptions` ADD CONSTRAINT `FK_users_TO_playlist_subscriptions_1`
	FOREIGN KEY (`subscriber_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

-- 이력 데이터 (물리 삭제 차단)
ALTER TABLE `reviews` ADD CONSTRAINT `FK_contents_TO_reviews_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `reviews` ADD CONSTRAINT `FK_users_TO_reviews_1`
	FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `watching_sessions` ADD CONSTRAINT `FK_users_TO_watching_sessions_1`
	FOREIGN KEY (`watcher_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `watching_sessions` ADD CONSTRAINT `FK_contents_TO_watching_sessions_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

-- 추천 데이터 (계정 삭제 시 함께 제거 — 개인 데이터)
ALTER TABLE `user_content_interactions` ADD CONSTRAINT `FK_users_TO_user_content_interactions_1`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `user_content_interactions` ADD CONSTRAINT `FK_contents_TO_user_content_interactions_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `user_preference_tag` ADD CONSTRAINT `FK_users_TO_user_preference_tag_1`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `user_preference_tag` ADD CONSTRAINT `FK_tags_TO_user_preference_tag_1`
	FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `user_similarities` ADD CONSTRAINT `FK_users_TO_user_similarities_1`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `user_similarities` ADD CONSTRAINT `FK_users_TO_user_similarities_2`
	FOREIGN KEY (`other_user_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

-- 메시지
ALTER TABLE `messages` ADD CONSTRAINT `FK_conversations_TO_messages_1`
	FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`)
	ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE `messages` ADD CONSTRAINT `FK_users_TO_messages_1`
	FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `messages` ADD CONSTRAINT `FK_users_TO_messages_2`
	FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`)
	ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE `messages` ADD CONSTRAINT `FK_contents_TO_messages_1`
	FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`)
	ON UPDATE RESTRICT ON DELETE SET NULL;