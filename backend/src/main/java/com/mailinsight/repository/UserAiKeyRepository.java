package com.mailinsight.repository;

import com.mailinsight.entity.User;
import com.mailinsight.entity.UserAiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAiKeyRepository extends JpaRepository<UserAiKey, UUID> {
    Optional<UserAiKey> findByUser(User user);

    boolean existsByUser(User user);
}
