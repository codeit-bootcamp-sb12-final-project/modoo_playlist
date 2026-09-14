package com.codeit.modoo_playlist.moduleapi.domain.tag.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.tag.repository.jpa.TagRepository;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock private TagRepository tagRepository;
    @InjectMocks private TagServiceImpl tagService;

    @Test
    void 태그명을_trim하고_대소문자_중복을_제거한_뒤_재조회한다() {
        Tag action = tag("Action");
        Tag drama = tag("Drama");
        when(tagRepository.findAllByNameIn(List.of("Action", "Drama"))).thenReturn(List.of(drama, action));

        List<Tag> result = tagService.getOrCreateTags(List.of("  Drama ", "Action", "action"));

        assertThat(result).containsExactly(action, drama);
        verify(tagRepository).insertIfAbsent(anyString(), eq("Action"), eq(TagKind.KEYWORD.name()));
        verify(tagRepository).insertIfAbsent(anyString(), eq("Drama"), eq(TagKind.KEYWORD.name()));
    }

    @Test
    void null_목록은_빈_결과를_반환하고_DB를_호출하지_않는다() {
        assertThat(tagService.getOrCreateTags(null)).isEmpty();
        verify(tagRepository, never()).insertIfAbsent(anyString(), anyString(), anyString());
    }

    @Test
    void 공백_태그명은_거부한다() {
        assertTagNameInvalid(List.of(" "), "blank");
    }

    @Test
    void 길이가_50자를_초과한_태그명은_거부한다() {
        assertTagNameInvalid(List.of("a".repeat(51)), "tooLong");
    }

    @Test
    void 저장_후_태그를_모두_재조회하지_못하면_동기화_예외가_발생한다() {
        when(tagRepository.findAllByNameIn(List.of("Action"))).thenReturn(List.of());

        assertThatThrownBy(() -> tagService.getOrCreateTags(List.of("Action")))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_SYNC_FAILED));
    }

    @Test
    void 삽입에는_UUID_문자열을_사용한다() {
        when(tagRepository.findAllByNameIn(List.of("Action"))).thenReturn(List.of(tag("Action")));
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);

        tagService.getOrCreateTags(List.of("Action"));

        verify(tagRepository).insertIfAbsent(idCaptor.capture(), eq("Action"), eq("KEYWORD"));
        assertThat(UUID.fromString(idCaptor.getValue())).isNotNull();
    }

    private void assertTagNameInvalid(List<String> names, String reason) {
        assertThatThrownBy(() -> tagService.getOrCreateTags(names))
                .isInstanceOfSatisfying(BaseException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TAG_NAME_INVALID);
                    assertThat(exception.getDetails()).containsEntry("reason", reason);
                });
    }

    private Tag tag(String name) {
        return Tag.builder().id(UUID.randomUUID()).name(name).kind(TagKind.KEYWORD).build();
    }
}
