package com.codeit.modoo_playlist.moduleapi.domain.content.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.codeit.modoo_playlist.moduleapi.domain.content.service.ContentService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;

@ExtendWith(MockitoExtension.class)
class ContentControllerTest {

    @Mock private ContentService contentService;
    @InjectMocks private ContentController controller;

    @Test
    void 목록조회는_서비스_결과를_200으로_반환한다() {
        ContentListRequest request = new ContentListRequest(null, null, null, null, null, null, null, null);
        ContentCursorResponse expected = new ContentCursorResponse(List.of(), null, null, false, 0,
                "watcherCount", "DESCENDING");
        UUID userId = UUID.randomUUID();
        UserDetails user = new UserDetails(
                new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null),
                "password"
        );
        when(contentService.getContents(request, userId)).thenReturn(expected);

        ResponseEntity<ContentCursorResponse> response = controller.getContents(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void 생성은_서비스_결과를_201로_반환한다() {
        ContentCreateRequest request = new ContentCreateRequest("movie", "제목", null, List.of());
        ContentDetailResponse expected = detail(UUID.randomUUID());
        when(contentService.createContent(request, null)).thenReturn(expected);

        ResponseEntity<ContentDetailResponse> response = controller.createContent(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void 상세조회는_로그인사용자와_콘텐츠ID를_서비스에_전달한다() {
        UUID contentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserDetails user = new UserDetails(
                new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null),
                "password"
        );
        ContentDetailResponse expected = detail(contentId);
        when(contentService.getContent(contentId, userId)).thenReturn(expected);

        ResponseEntity<ContentDetailResponse> response = controller.getContent(contentId, user);

        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void 수정과_삭제는_서비스에_위임하고_정해진_상태를_반환한다() {
        UUID id = UUID.randomUUID();
        ContentUpdateRequest request = new ContentUpdateRequest("수정", null, null);
        ContentDetailResponse expected = detail(id);
        when(contentService.updateContent(id, request, null)).thenReturn(expected);

        assertThat(controller.updateContent(id, request, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteContent(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(contentService).deleteContent(id);
    }

    @Test
    void 조회는_USER_쓰기작업은_ADMIN_권한을_요구한다() throws Exception {
        assertThat(authority("getContents", ContentListRequest.class, UserDetails.class))
                .isEqualTo("hasRole('USER')");
        assertThat(authority("getContent", UUID.class, UserDetails.class)).isEqualTo("hasRole('USER')");
        assertThat(authority("createContent", ContentCreateRequest.class,
                org.springframework.web.multipart.MultipartFile.class)).isEqualTo("hasRole('ADMIN')");
        assertThat(authority("updateContent", UUID.class, ContentUpdateRequest.class,
                org.springframework.web.multipart.MultipartFile.class)).isEqualTo("hasRole('ADMIN')");
        assertThat(authority("deleteContent", UUID.class)).isEqualTo("hasRole('ADMIN')");
    }

    private String authority(String method, Class<?>... parameterTypes) throws Exception {
        return ContentController.class.getMethod(method, parameterTypes)
                .getAnnotation(PreAuthorize.class).value();
    }

    private ContentDetailResponse detail(UUID id) {
        return new ContentDetailResponse(id, "movie", "제목", null, null, List.of(),
                BigDecimal.ZERO, 0, 0, null, null, null, null, List.of(), null);
    }
}
