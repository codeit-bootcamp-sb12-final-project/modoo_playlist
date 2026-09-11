package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

public interface PlaylistQueryRepository {

    PlaylistQueryPage findAllByCondition(PlaylistListCondition condition);

}