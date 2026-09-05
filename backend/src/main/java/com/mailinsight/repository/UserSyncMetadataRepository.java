package com.mailinsight.repository;

import com.mailinsight.entity.ConnectedAccount;
import com.mailinsight.entity.User;
import com.mailinsight.entity.UserSyncMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSyncMetadataRepository extends JpaRepository<UserSyncMetadata, UUID> {
    Optional<UserSyncMetadata> findByAccount(ConnectedAccount account);

    List<UserSyncMetadata> findAllByUser(User user);
}
