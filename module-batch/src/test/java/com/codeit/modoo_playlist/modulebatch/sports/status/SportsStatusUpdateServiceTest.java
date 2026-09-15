package com.codeit.modoo_playlist.modulebatch.sports.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

@ExtendWith(MockitoExtension.class)
class SportsStatusUpdateServiceTest {

    @Mock private SportsContentMapper mapper;

    @Test
    void 종목별_경기시간을_Mapper에_전달하고_변경건수를_반환한다() {
        SportsBatchProperties properties = new SportsBatchProperties();
        when(mapper.updateCalculatedStatuses(any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(3);

        int updated = new SportsStatusUpdateService(mapper, properties).updateStatuses();

        assertThat(updated).isEqualTo(3);
        verify(mapper).updateCalculatedStatuses(any(),
                org.mockito.ArgumentMatchers.eq(properties.getSoccerDurationMinutes()),
                org.mockito.ArgumentMatchers.eq(properties.getBasketballDurationMinutes()),
                org.mockito.ArgumentMatchers.eq(properties.getBaseballDurationMinutes()),
                org.mockito.ArgumentMatchers.eq(properties.getDefaultDurationMinutes()));
    }
}
