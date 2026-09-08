package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.core.domain.search.entity.SearchLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {

}
