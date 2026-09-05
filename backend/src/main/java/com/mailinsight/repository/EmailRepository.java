package com.mailinsight.repository;

import com.mailinsight.entity.Email;
import com.mailinsight.entity.User;
import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    boolean existsByGmailMessageId(String gmailMessageId);

    Page<Email> findAllByUserOrderByReceivedAtDesc(User user, Pageable pageable);

    Page<Email> findAllByUserAndCategoryOrderByReceivedAtDesc(User user, EmailCategory category, Pageable pageable);

    Page<Email> findAllByUserAndPriorityOrderByReceivedAtDesc(User user, EmailPriority priority, Pageable pageable);

    @Query("SELECT e.category, COUNT(e) FROM Email e WHERE e.user = :user GROUP BY e.category")
    List<Object[]> countByCategoryForUser(@Param("user") User user);

    @Query("SELECT e.priority, COUNT(e) FROM Email e WHERE e.user = :user GROUP BY e.priority")
    List<Object[]> countByPriorityForUser(@Param("user") User user);

    long countByUser(User user);

    Page<Email> findAllByUserAndSourceAccountOrderByReceivedAtDesc(User user, String sourceAccount, Pageable pageable);
}
