package com.mailanalyzer.controller;

import com.mailanalyzer.dto.response.AnalyzeResultResponse;
import com.mailanalyzer.dto.response.DashboardStatsResponse;
import com.mailanalyzer.dto.response.EmailResponse;
import com.mailanalyzer.dto.response.NewEmailCountResponse;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;
import com.mailanalyzer.mapper.EmailMapper;
import com.mailanalyzer.service.EmailService;
import com.mailanalyzer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all email-related endpoints.
 *
 * <h3>Endpoints</h3>
 * <pre>
 * GET  /api/emails/new-count              → count of unanalyzed emails
 * POST /api/emails/analyze                → trigger full AI analysis pipeline
 * GET  /api/emails                        → paginated list of all analyzed emails
 * GET  /api/emails/category/{category}    → paginated emails by category
 * GET  /api/emails/priority/{priority}    → paginated emails by priority (1–5)
 * GET  /api/emails/stats                  → dashboard stats (pie + bar chart data)
 * </pre>
 *
 * <p>The authenticated user is always resolved from the Spring Security
 * {@code Authentication} context. User IDs are NEVER accepted from request
 * parameters or request bodies.
 */
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE     = 100;

    private final EmailService emailService;
    private final UserService  userService;
    private final EmailMapper  emailMapper;

    // ── GET /api/emails/new-count ─────────────────────────────────────────

    /**
     * Returns the count of new (un-analyzed) emails across all connected
     * Gmail accounts.
     *
     * <p>The frontend uses {@code analyzeEnabled} from the response to
     * enable/disable the "Analyze Emails" button. If {@code count == 0},
     * no Gemini request will be made on click.
     */
    @GetMapping("/new-count")
    public ResponseEntity<NewEmailCountResponse> getNewEmailCount(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.getNewEmailCount(user));
    }

    // ── POST /api/emails/analyze ──────────────────────────────────────────

    /**
     * Triggers the full AI email analysis pipeline:
     * Gmail → batch → Gemini → validate → persist.
     *
     * <p>Returns immediately with the count of emails analyzed and skipped.
     * This is a synchronous endpoint; for large batches it may take a few
     * seconds while waiting for Gemini.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResultResponse> analyzeEmails(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.analyzeEmails(user));
    }

    // ── GET /api/emails ───────────────────────────────────────────────────

    /**
     * Returns a paginated list of all analyzed emails for the authenticated
     * user, ordered by received date descending (newest first).
     *
     * @param page zero-based page number (default 0)
     * @param size number of items per page (default 20, max 100)
     */
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

    // ── GET /api/emails/category/{category} ──────────────────────────────

    /**
     * Returns a paginated list of emails filtered by the given category.
     *
     * <p>Used by the dashboard's category click-through feature.
     *
     * @param category one of the {@link EmailCategory} enum values (case-insensitive)
     */
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

    // ── GET /api/emails/priority/{priority} ──────────────────────────────

    /**
     * Returns a paginated list of emails filtered by priority level (1–5).
     *
     * @param priority integer between 1 and 5 inclusive
     */
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

    // ── GET /api/emails/stats ─────────────────────────────────────────────

    /**
     * Returns dashboard statistics for the authenticated user:
     * <ul>
     *   <li>Total analyzed email count</li>
     *   <li>Count per {@link EmailCategory} (for pie chart)</li>
     *   <li>Count per {@link EmailPriority} (for bar chart)</li>
     *   <li>Number of connected Gmail accounts</li>
     * </ul>
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailService.getStats(user));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /** Prevents clients from requesting unreasonably large pages. */
    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
