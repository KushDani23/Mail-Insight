package com.mailanalyzer.mapper;

import com.mailanalyzer.dto.response.AccountResponse;
import com.mailanalyzer.dto.response.EmailResponse;
import com.mailanalyzer.dto.response.UserResponse;
import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.Email;
import com.mailanalyzer.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting JPA entities to response DTOs.
 *
 * <p>We use manual mapping (no MapStruct) to keep the project simple and
 * transparent for beginners.  Each method documents which fields it maps.
 */
@Component
public class EmailMapper {

    /**
     * Maps a {@link User} entity to a {@link UserResponse} DTO.
     * Safe to return to the frontend – no sensitive fields.
     */
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getCreatedAt()
        );
    }

    /**
     * Maps a {@link ConnectedAccount} to an {@link AccountResponse} DTO.
     * Tokens (accessToken, refreshToken) are intentionally excluded.
     */
    public AccountResponse toAccountResponse(ConnectedAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getGmailAddress(),
                account.getCreatedAt()
        );
    }

    /**
     * Maps an {@link Email} entity to an {@link EmailResponse} DTO.
     * The raw email body is never stored in Email, so it cannot leak here.
     */
    public EmailResponse toEmailResponse(Email email) {
        return new EmailResponse(
                email.getId(),
                email.getGmailMessageId(),
                email.getSourceAccount(),
                email.getSender(),
                email.getSubject(),
                email.getSummary(),
                email.getCategory(),
                email.getPriority(),
                email.getReceivedAt(),
                email.getAnalyzedAt()
        );
    }
}
