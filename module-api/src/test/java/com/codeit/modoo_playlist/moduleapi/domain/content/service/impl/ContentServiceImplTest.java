package com.codeit.modoo_playlist.moduleapi.domain.content.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentSource;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentPersonRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentSportsRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentVideoRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage.ContentItem;
import com.codeit.modoo_playlist.moduleapi.domain.content.storage.ThumbnailStorage;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.tag.service.TagService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentPersonRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentSportsRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentVideoRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    private final UUID userId = UUID.randomUUID();

    @Mock private ContentRepository contentRepository;
    @Mock private ContentTagRepository contentTagRepository;
    @Mock private ContentVideoRepository contentVideoRepository;
    @Mock private ContentSportsRepository contentSportsRepository;
    @Mock private ContentPersonRepository contentPersonRepository;
    @Mock private UserContentInteractionRepository userContentInteractionRepository;
    @Mock private TagService tagService;
    @Mock private ContentMapper contentMapper;
    @Mock private ThumbnailStorage thumbnailStorage;
    @InjectMocks private ContentServiceImpl contentService;

    @Test
    void 목록조회_기본값은_시청자수_내림차순과_20개다() {
        ContentQueryPage page = new ContentQueryPage(List.of(), null, null, false, 0);
        ContentCursorResponse expected = new ContentCursorResponse(
                List.of(), null, null, false, 0, "watcherCount", "DESCENDING"
        );
        ArgumentCaptor<ContentListCondition> captor = ArgumentCaptor.forClass(ContentListCondition.class);
        when(contentRepository.findAllByCondition(any(ContentListCondition.class), eq(userId))).thenReturn(page);
        when(contentMapper.toCursorResponse(page, List.of(), "watcherCount", "DESCENDING"))
                .thenReturn(expected);

        ContentCursorResponse result = contentService.getContents(
                new ContentListRequest(null, null, null, null, null, null, null, null), userId
        );

        assertThat(result).isSameAs(expected);
        verify(contentRepository).findAllByCondition(captor.capture(), eq(userId));
        assertThat(captor.getValue().limit()).isEqualTo(20);
        assertThat(captor.getValue().sortBy()).isEqualTo(ContentListCondition.SortType.WATCHER_COUNT);
        assertThat(captor.getValue().sortDirection())
                .isEqualTo(ContentListCondition.SortDirection.DESCENDING);
    }

    @Test
    void 추천순은_로그인사용자와_RECOMMENDED_조건을_Repository에_전달한다() {
        ContentQueryPage page = new ContentQueryPage(List.of(), null, null, false, 0);
        ContentCursorResponse expected = new ContentCursorResponse(
                List.of(), null, null, false, 0, "recommended", "DESCENDING"
        );
        ArgumentCaptor<ContentListCondition> captor = ArgumentCaptor.forClass(ContentListCondition.class);
        when(contentRepository.findAllByCondition(any(ContentListCondition.class), eq(userId)))
                .thenReturn(page);
        when(contentMapper.toCursorResponse(page, List.of(), "recommended", "DESCENDING"))
                .thenReturn(expected);

        ContentCursorResponse result = contentService.getContents(
                new ContentListRequest(
                        null, null, null, null, null, 20, "DESCENDING", "recommended"
                ),
                userId
        );

        assertThat(result).isSameAs(expected);
        verify(contentRepository).findAllByCondition(captor.capture(), eq(userId));
        assertThat(captor.getValue().sortBy()).isEqualTo(ContentListCondition.SortType.RECOMMENDED);
    }

    @Test
    void TMDB_영화는_장르를_이름순으로_최대_4개만_노출한다() {
        Content content = content(ContentType.MOVIE, ContentSource.TMDB);
        List<Tag> tags = List.of(
                tag("Thriller", TagKind.GENRE), tag("keyword", TagKind.KEYWORD),
                tag("Comedy", TagKind.GENRE), tag("Fantasy", TagKind.GENRE),
                tag("Action", TagKind.GENRE), tag("Drama", TagKind.GENRE)
        );
        ContentQueryPage page = new ContentQueryPage(List.of(new ContentItem(content, 7L)), null, null, false, 1);
        List<String> displayTags = List.of("Action", "Comedy", "Drama", "Fantasy");
        ContentListItemResponse item = new ContentListItemResponse(
                content.getId(), "movie", "제목", null, null, null,
                displayTags, BigDecimal.ZERO, 0, 7L
        );
        ContentCursorResponse expected = new ContentCursorResponse(
                List.of(item), null, null, false, 1, "watcherCount", "DESCENDING"
        );
        when(contentRepository.findAllByCondition(any(), eq(userId))).thenReturn(page);
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId())))
                .thenReturn(tags.stream().map(tag -> contentTag(content, tag)).toList());
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentMapper.toListItem(content, displayTags, 7L)).thenReturn(item);
        when(contentMapper.toCursorResponse(page, List.of(item), "watcherCount", "DESCENDING"))
                .thenReturn(expected);

        assertThat(contentService.getContents(
                new ContentListRequest(null, null, null, null, null, null, null, null), userId
        )).isSameAs(expected);
    }

    @Test
    void 콘텐츠_수정에서_태그_차이와_콘텐츠수를_함께_반영한다() {
        Content content = content(ContentType.MOVIE, null);
        Tag retained = tag("Action", TagKind.GENRE);
        Tag removed = tag("Drama", TagKind.GENRE);
        Tag added = tag("Comedy", TagKind.GENRE);
        ContentTag retainedLink = contentTag(content, retained);
        ContentTag removedLink = contentTag(content, removed);
        ContentDetailResponse expected = detail(content.getId());
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));
        when(tagService.getOrCreateTags(List.of("Action", "Comedy"))).thenReturn(List.of(retained, added));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId())))
                .thenReturn(List.of(retainedLink, removedLink))
                .thenReturn(List.of(retainedLink, contentTag(content, added)));
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(content.getId()))
                .thenReturn(List.of());
        when(contentVideoRepository.findById(content.getId())).thenReturn(Optional.empty());
        when(contentMapper.toDetail(eq(content), anyList(), eq(0L), isNull(), isNull(), anyList(), isNull()))
                .thenReturn(expected);

        ContentDetailResponse result = contentService.updateContent(
                content.getId(),
                new ContentUpdateRequest("수정 제목", "수정 설명", List.of("Action", "Comedy")),
                null
        );

        assertThat(result).isSameAs(expected);
        assertThat(content.getTitle()).isEqualTo("수정 제목");
        verify(contentTagRepository).deleteAll(List.of(removedLink));
        verify(contentTagRepository).decreaseTagContentCounts(List.of(removed.getId()));
        verify(contentTagRepository).increaseTagContentCounts(List.of(added.getId()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ContentTag>> addedCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(contentTagRepository).saveAll(addedCaptor.capture());
        assertThat(addedCaptor.getValue()).singleElement().satisfies(link -> {
            assertThat(link.getContent()).isSameAs(content);
            assertThat(link.getTag()).isSameAs(added);
        });
    }

    @Test
    void 썸네일_저장실패는_FILE_SAVE_FAILED로_변환한다() throws IOException {
        Content content = content(ContentType.MOVIE, null);
        MultipartFile thumbnail = org.mockito.Mockito.mock(MultipartFile.class);
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));
        when(thumbnail.isEmpty()).thenReturn(false);
        when(thumbnailStorage.store(thumbnail)).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> contentService.updateContent(
                content.getId(), new ContentUpdateRequest(null, null, null), thumbnail
        )).isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_SAVE_FAILED));
        verify(tagService, never()).getOrCreateTags(anyList());
    }

    @Test
    void 삭제는_실제삭제가_아닌_softDelete로_처리한다() {
        Content content = content(ContentType.MOVIE, null);
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));

        contentService.deleteContent(content.getId());

        assertThat(content.getDeletedAt()).isNotNull();
        verify(contentRepository, never()).delete(any(Content.class));
    }

    @Test
    void 없는_콘텐츠_조회는_CONTENT_NOT_FOUND를_반환한다() {
        UUID id = UUID.randomUUID();
        when(contentRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getContent(id, userId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
    }

    @Test
    void 콘텐츠_상세조회는_현재_사용자의_반응을_포함한다() {
        Content content = content(ContentType.MOVIE, null);
        UserContentInteraction interaction = UserContentInteraction.builder()
                .type(InteractionType.LIKE)
                .build();
        ContentDetailResponse expected = detail(content.getId(), InteractionType.LIKE);
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId()))
                .thenReturn(Optional.of(content));
        when(userContentInteractionRepository.findByUserIdAndContentIdAndTypeIn(
                eq(userId), eq(content.getId()), any()))
                .thenReturn(Optional.of(interaction));
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(content.getId()))
                .thenReturn(List.of());
        when(contentVideoRepository.findById(content.getId())).thenReturn(Optional.empty());
        when(contentMapper.toDetail(
                eq(content), anyList(), eq(0L), isNull(), isNull(), anyList(), eq(InteractionType.LIKE)))
                .thenReturn(expected);

        ContentDetailResponse result = contentService.getContent(content.getId(), userId);

        assertThat(result.myReaction()).isEqualTo(InteractionType.LIKE);
    }

    @Test
    void 관리자_콘텐츠_생성은_Content만_저장하고_요청한_태그를_연결한다() {
        UUID id = UUID.randomUUID();
        Tag action = tag("Action", TagKind.KEYWORD);
        ContentDetailResponse expected = detail(id);
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });
        when(tagService.getOrCreateTags(List.of("Action"))).thenReturn(List.of(action));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(id)))
                .thenReturn(List.of())
                .thenReturn(List.of(contentTag(
                        Content.builder().id(id).type(ContentType.MOVIE).title("관리자 콘텐츠").build(), action
                )));
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(id)).thenReturn(List.of());
        when(contentVideoRepository.findById(id)).thenReturn(Optional.empty());
        when(contentMapper.toDetail(any(Content.class), anyList(), eq(0L), isNull(), isNull(), anyList(), isNull()))
                .thenReturn(expected);

        ContentDetailResponse result = contentService.createContent(
                new ContentCreateRequest("movie", "관리자 콘텐츠", "설명", List.of("Action")), null
        );

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Content> contentCaptor = ArgumentCaptor.forClass(Content.class);
        verify(contentRepository).save(contentCaptor.capture());
        assertThat(contentCaptor.getValue()).satisfies(saved -> {
            assertThat(saved.getType()).isEqualTo(ContentType.MOVIE);
            assertThat(saved.getTitle()).isEqualTo("관리자 콘텐츠");
            assertThat(saved.getDescription()).isEqualTo("설명");
            assertThat(saved.getSource()).isNull();
        });
        verify(contentTagRepository).increaseTagContentCounts(List.of(action.getId()));
    }

    @Test
    void 지원하지_않는_콘텐츠_타입은_저장하지_않는다() {
        assertThatThrownBy(() -> contentService.createContent(
                new ContentCreateRequest("book", "제목", null, List.of()), null
        )).isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_TYPE_INVALID));

        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void 스포츠_생성은_필수_상세정보가_없으면_저장하지_않는다() {
        ContentCreateRequest request = new ContentCreateRequest(
                "sport", "경기", null, null, null, List.of(), null,
                new ContentSportsRequest("Soccer", null, null, "홈", "원정",
                        null, null, Instant.now()),
                null
        );

        assertThatThrownBy(() -> contentService.createContent(request, null))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_DETAIL_INVALID));
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void 스포츠에_비디오나_등장인물을_함께_보내면_거부한다() {
        ContentVideoRequest video = new ContentVideoRequest(
                null, null, null, null, null, null, null, null, null, null
        );
        ContentSportsRequest sports = new ContentSportsRequest(
                "Soccer", "Premier League", null, "Liverpool", "Fulham",
                null, null, Instant.now()
        );
        ContentPersonRequest person = new ContentPersonRequest("CAST", "선수", null, null, null);
        ContentCreateRequest request = new ContentCreateRequest(
                "sport", "경기", null, null, null, List.of(), video, sports, List.of(person)
        );

        assertThatThrownBy(() -> contentService.createContent(request, null))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_DETAIL_INVALID));
        verify(contentRepository, never()).save(any(Content.class));
    }

    @Test
    void 스포츠_생성은_콘텐츠와_스포츠상세를_함께_저장한다() {
        UUID id = UUID.randomUUID();
        Instant kickoffAt = Instant.parse("2026-09-16T10:00:00Z");
        ContentSportsRequest sports = new ContentSportsRequest(
                "Soccer", "Premier League", "2026-2027", "Liverpool", "Fulham",
                "Anfield", null, kickoffAt
        );
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });

        contentService.createContent(new ContentCreateRequest(
                "sport", "Liverpool vs Fulham", null, null, null,
                List.of(), null, sports, null
        ), null);

        ArgumentCaptor<ContentSports> captor = ArgumentCaptor.forClass(ContentSports.class);
        verify(contentSportsRepository).save(captor.capture());
        assertThat(captor.getValue()).satisfies(saved -> {
            assertThat(saved.getContent().getId()).isEqualTo(id);
            assertThat(saved.getSportType()).isEqualTo("Soccer");
            assertThat(saved.getLeague()).isEqualTo("Premier League");
            assertThat(saved.getStatus()).isEqualTo(com.codeit.modoo_playlist.core.domain.content.type.SportsStatus.SCHEDULED);
            assertThat(saved.getKickoffAt()).isEqualTo(kickoffAt);
        });
        verify(contentVideoRepository, never()).save(any());
        verify(contentPersonRepository, never()).saveAll(any());
    }

    @Test
    void 영상_생성은_선택한_상세정보와_등장인물을_함께_저장한다() {
        UUID id = UUID.randomUUID();
        ContentVideoRequest video = new ContentVideoRequest(
                120, null, "tt123", null, null, null, "ko", null,
                new BigDecimal("8.2"), 100
        );
        List<ContentPersonRequest> people = List.of(
                new ContentPersonRequest("ACTOR", "배우", "주인공", "person-1", "https://image/1.jpg")
        );
        when(contentRepository.save(any(Content.class))).thenAnswer(invocation -> {
            Content saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });

        contentService.createContent(new ContentCreateRequest(
                "movie", "영화", null, null, "KR", List.of(), video, null, people
        ), null);

        ArgumentCaptor<com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo> videoCaptor =
                ArgumentCaptor.forClass(com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo.class);
        verify(contentVideoRepository).save(videoCaptor.capture());
        assertThat(videoCaptor.getValue().getRuntimeMinutes()).isEqualTo(120);
        assertThat(videoCaptor.getValue().getExternalRating()).isEqualByComparingTo("8.2");
        verify(contentPersonRepository).deleteAllByContent_Id(id);
        verify(contentPersonRepository).saveAll(any());
        verify(contentSportsRepository, never()).save(any());
    }

    @Test
    void 수정요청의_tags가_null이면_기존_태그를_변경하지_않는다() {
        Content content = content(ContentType.MOVIE, null);
        ContentDetailResponse expected = detail(content.getId());
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId()))).thenReturn(List.of());
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(content.getId()))
                .thenReturn(List.of());
        when(contentVideoRepository.findById(content.getId())).thenReturn(Optional.empty());
        when(contentMapper.toDetail(eq(content), anyList(), eq(0L), isNull(), isNull(), anyList(), isNull()))
                .thenReturn(expected);

        contentService.updateContent(
                content.getId(), new ContentUpdateRequest("새 제목", null, null), null
        );

        verify(tagService, never()).getOrCreateTags(anyList());
        verify(contentTagRepository, never()).saveAll(any());
        verify(contentTagRepository, never()).deleteAll(any());
    }

    @Test
    void 비디오_상세정보는_전달한_필드만_수정한다() {
        Content content = content(ContentType.MOVIE, null);
        ContentVideo video = ContentVideo.builder()
                .content(content)
                .runtimeMinutes(120)
                .imdbId("tt123")
                .originalLanguage("en")
                .popularity(10.5f)
                .externalRating(new BigDecimal("7.1"))
                .externalRatingCount(200)
                .build();
        ContentDetailResponse expected = detail(content.getId());
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId()))).thenReturn(List.of());
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(content.getId()))
                .thenReturn(List.of());
        when(contentVideoRepository.findById(content.getId())).thenReturn(Optional.of(video));
        when(contentMapper.toDetail(eq(content), anyList(), eq(0L), eq(video), isNull(), anyList(), isNull()))
                .thenReturn(expected);

        contentService.updateContent(content.getId(), new ContentUpdateRequest(
                null, null, null, null, null,
                new ContentVideoRequest(null, null, null, null, null, null, null,
                        12.5f, null, null),
                null, null
        ), null);

        assertThat(video.getRuntimeMinutes()).isEqualTo(120);
        assertThat(video.getImdbId()).isEqualTo("tt123");
        assertThat(video.getOriginalLanguage()).isEqualTo("en");
        assertThat(video.getPopularity()).isEqualTo(12.5f);
        assertThat(video.getExternalRating()).isEqualByComparingTo("7.1");
        assertThat(video.getExternalRatingCount()).isEqualTo(200);
    }

    @Test
    void 등장인물은_요청이_있을_때만_입력순서대로_전체교체한다() {
        Content content = content(ContentType.TV, null);
        ContentDetailResponse expected = detail(content.getId());
        List<ContentPersonRequest> people = List.of(
                new ContentPersonRequest("ACTOR", "첫 배우", "주인공", "p1", "https://image/1.jpg"),
                new ContentPersonRequest(null, "둘째 배우", null, null, null)
        );
        when(contentRepository.findByIdAndDeletedAtIsNull(content.getId())).thenReturn(Optional.of(content));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId()))).thenReturn(List.of());
        when(contentSportsRepository.findAllById(List.of())).thenReturn(List.of());
        when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(content.getId()))
                .thenReturn(List.of());
        when(contentVideoRepository.findById(content.getId())).thenReturn(Optional.empty());
        when(contentMapper.toDetail(eq(content), anyList(), eq(0L), isNull(), isNull(), anyList(), isNull()))
                .thenReturn(expected);

        contentService.updateContent(content.getId(), new ContentUpdateRequest(
                null, null, null, null, null, null, null, people
        ), null);

        verify(contentPersonRepository).deleteAllByContent_Id(content.getId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(contentPersonRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).satisfiesExactly(
                first -> {
                    assertThat(first.getPersonName()).isEqualTo("첫 배우");
                    assertThat(first.getDisplayOrder()).isZero();
                    assertThat(first.getPersonImg()).isEqualTo("https://image/1.jpg");
                },
                second -> {
                    assertThat(second.getRoleType()).isEqualTo("CAST");
                    assertThat(second.getDisplayOrder()).isEqualTo(1);
                }
        );
    }

    @Test
    void 잘못된_정렬값과_방향은_각각_명확한_예외를_반환한다() {
        ContentListRequest invalidSort = new ContentListRequest(
                null, null, null, null, null, 20, "DESCENDING", "unknown"
        );
        ContentListRequest invalidDirection = new ContentListRequest(
                null, null, null, null, null, 20, "DOWN", "createdAt"
        );

        assertThatThrownBy(() -> contentService.getContents(invalidSort, userId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_SORT_INVALID));
        assertThatThrownBy(() -> contentService.getContents(invalidDirection, userId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_SORT_DIRECTION_INVALID));
    }

    @Test
    void TMDB_TV는_드라마_또는_애니메이션을_첫_표시태그로_사용한다() {
        Content content = content(ContentType.TV, ContentSource.TMDB);
        List<Tag> tags = List.of(
                tag("Fantasy", TagKind.GENRE), tag("Animation", TagKind.GENRE),
                tag("Comedy", TagKind.GENRE), tag("Family", TagKind.GENRE)
        );

        assertDisplayTags(content, tags, null,
                List.of("애니메이션", "Comedy", "Family", "Fantasy"));
    }

    @Test
    void 스포츠는_종목_리그_홈팀_원정팀_순으로_표시한다() {
        Content content = content(ContentType.SPORT, ContentSource.SPORTS_DB);
        ContentSports sports = ContentSports.builder()
                .contentId(content.getId()).content(content).sportType("Soccer")
                .league("Premier League").homeTeam("Arsenal").awayTeam("Chelsea")
                .kickoffAt(java.time.Instant.now()).build();

        assertDisplayTags(content, List.of(tag("Football", TagKind.GENRE)), sports,
                List.of("Soccer", "Premier League", "Arsenal", "Chelsea"));
    }

    @Test
    void 관리자_생성_콘텐츠는_입력한_태그_순서를_유지한다() {
        Content content = content(ContentType.MOVIE, null);
        assertDisplayTags(content,
                List.of(tag("두번째", TagKind.KEYWORD), tag("첫번째", TagKind.KEYWORD)),
                null, List.of("두번째", "첫번째"));
    }

    private void assertDisplayTags(
            Content content, List<Tag> tags, ContentSports sports, List<String> expectedTags
    ) {
        ContentQueryPage page = new ContentQueryPage(List.of(new ContentItem(content, 0)), null, null, false, 1);
        ContentListItemResponse item = new ContentListItemResponse(
                content.getId(), "type", "제목", null, null, null,
                expectedTags, BigDecimal.ZERO, 0, 0
        );
        when(contentRepository.findAllByCondition(any(), eq(userId))).thenReturn(page);
        when(contentTagRepository.findAllWithTagByContentIds(List.of(content.getId())))
                .thenReturn(tags.stream().map(tag -> contentTag(content, tag)).toList());
        when(contentSportsRepository.findAllById(
                content.getType() == ContentType.SPORT ? List.of(content.getId()) : List.of()
        )).thenReturn(sports == null ? List.of() : List.of(sports));
        when(contentMapper.toListItem(content, expectedTags, 0)).thenReturn(item);
        when(contentMapper.toCursorResponse(page, List.of(item), "watcherCount", "DESCENDING"))
                .thenReturn(new ContentCursorResponse(
                        List.of(item), null, null, false, 1, "watcherCount", "DESCENDING"
                ));

        ContentCursorResponse response = contentService.getContents(
                new ContentListRequest(null, null, null, null, null, null, null, null), userId
        );
        assertThat(response.data().get(0).tags()).containsExactlyElementsOf(expectedTags);
    }

    private Content content(ContentType type, ContentSource source) {
        return Content.builder().id(UUID.randomUUID()).type(type).title("제목").source(source).build();
    }

    private Tag tag(String name, TagKind kind) {
        return Tag.builder().id(UUID.randomUUID()).name(name).kind(kind).build();
    }

    private ContentTag contentTag(Content content, Tag tag) {
        return ContentTag.builder()
                .id(new ContentTagId(content.getId(), tag.getId())).content(content).tag(tag).build();
    }

    private ContentDetailResponse detail(UUID id) {
        return detail(id, null);
    }

    private ContentDetailResponse detail(UUID id, InteractionType myReaction) {
        return new ContentDetailResponse(
                id, "movie", "제목", null, null, List.of(), BigDecimal.ZERO,
                0, 0, null, null, null, null, List.of(), myReaction
        );
    }
}
