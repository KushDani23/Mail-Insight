package com.mailinsight.mapper;

import com.mailinsight.dto.response.AccountResponse;
import com.mailinsight.dto.response.EmailResponse;
import com.mailinsight.dto.response.UserResponse;
import com.mailinsight.entity.ConnectedAccount;
import com.mailinsight.entity.Email;
import com.mailinsight.entity.User;
import org.springframework.stereotype.Component;

@Component
public class EmailMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(),
                user.getPictureUrl(), user.getCreatedAt());
    }

    public AccountResponse toAccountResponse(ConnectedAccount account) {
        return new AccountResponse(account.getId(), account.getGmailAddress(), account.getCreatedAt());
    }

    public EmailResponse toEmailResponse(Email email) {
        return new EmailResponse(
                email.getId(), email.getGmailMessageId(),
                email.getSourceAccount(), email.getSender(),
                email.getSubject(), email.getSummary(),
                email.getCategory(), email.getPriority(),
                email.getReceivedAt(), email.getAnalyzedAt());
    }
}
