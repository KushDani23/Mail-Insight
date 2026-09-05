package com.mailinsight.controller;

import com.mailinsight.dto.response.AnalyzeResultResponse;
import com.mailinsight.dto.response.DashboardStatsResponse;
import com.mailinsight.dto.response.EmailResponse;
import com.mailinsight.dto.response.NewEmailCountResponse;
import com.mailinsight.entity.User;
import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import com.mailinsight.mapper.EmailMapper;
import com.mailinsight.service.EmailService;
import com.mailinsight.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmailService emailService;
    private final UserService userService;
    private final EmailMapper emailMapper;

    @GetMapping("/new-count")
    public ResponseEntity<NewEmailCountResponse> getNewEmailCount(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.getNewEmailCount(user));
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResultResponse> analyzeEmails(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.analyzeEmails(user));
    }

    @GetMapping
    public ResponseEntity<Page<EmailResponse>> getAllEmails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);
        Pageable pageable = PageRequest.of(page, clampSize(size));

        Page<EmailResponse> result = emailService.getEmails(user, pageable)
                .map(emailMapper::toEmailResponse);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<EmailResponse>> getEmailsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);
        EmailCategory emailCategory = EmailCategory.valueOf(category.toUpperCase());
        Pageable pageable = PageRequest.of(page, clampSize(size));

        Page<EmailResponse> result = emailService.getEmailsByCategory(user, emailCategory, pageable)
                .map(emailMapper::toEmailResponse);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<Page<EmailResponse>> getEmailsByPriority(
            @PathVariable int priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);
        EmailPriority emailPriority = EmailPriority.fromLevel(priority);
        Pageable pageable = PageRequest.of(page, clampSize(size));

        Page<EmailResponse> result = emailService.getEmailsByPriority(user, emailPriority, pageable)
                .map(emailMapper::toEmailResponse);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.getStats(user));
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
