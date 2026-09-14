package com.codeit.modoo_playlist.modulebatch.sports.status;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SportsStatusUpdateService {

    private final SportsContentMapper contentMapper;
    private final SportsBatchProperties properties;

    @Transactional
    public int updateStatuses() {
        return contentMapper.updateCalculatedStatuses(
                Instant.now(),
                properties.getSoccerDurationMinutes(),
                properties.getBasketballDurationMinutes(),
                properties.getBaseballDurationMinutes(),
                properties.getDefaultDurationMinutes()
        );
    }
}
