package com.mailanalyzer.repository;

import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.entity.UserSyncMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserSyncMetadata} – tracks Gmail History ID
 * per connected account for incremental sync.
 */
@Repository
public interface UserSyncMetadataRepository extends JpaRepository<UserSyncMetadata, UUID> {

    /**
     * Find the sync metadata for a specific connected account.
     * Returns empty if this is the first sync (no historyId saved yet).
     */
    Optional<UserSyncMetadata> findByAccount(ConnectedAccount account);

    /**
     * Find all sync metadata records belonging to a user's accounts.
     * Used when preparing to sync all connected accounts in one pass.
     */
    List<UserSyncMetadata> findAllByUser(User user);
}
