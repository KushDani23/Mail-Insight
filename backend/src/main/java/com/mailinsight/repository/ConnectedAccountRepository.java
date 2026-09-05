package com.mailinsight.repository;

import com.mailinsight.entity.ConnectedAccount;
import com.mailinsight.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, UUID> {

    List<ConnectedAccount> findAllByUser(User user);

    Optional<ConnectedAccount> findByUserAndGmailAddress(User user, String gmailAddress);

    Optional<ConnectedAccount> findFirstByUserOrderByCreatedAtAsc(User user);

    boolean existsByUserAndGmailAddress(User user, String gmailAddress);

    long countByUser(User user);
}
