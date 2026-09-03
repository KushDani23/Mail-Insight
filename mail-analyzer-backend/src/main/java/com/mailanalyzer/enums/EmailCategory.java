package com.mailanalyzer.enums;

/**
 * All supported email categories.
 *
 * <p>These are the ONLY valid values that Gemini is allowed to return.
 * {@link com.mailanalyzer.util.GeminiResponseParser} rejects any response
 * that contains a category not in this enum.
 *
 * <p>Priority mapping (driven by business rules, NOT by the AI):
 * <ul>
 *   <li>Priority 1 – time-sensitive / high-impact</li>
 *   <li>Priority 2 – learning & growth</li>
 *   <li>Priority 3 – content & community</li>
 *   <li>Priority 4 – marketing & noise</li>
 *   <li>Priority 5 – club & social activities</li>
 * </ul>
 */
public enum EmailCategory {

    // ── Priority 1 ──────────────────────────────────────────────────────
    CAREER_OPPORTUNITIES,
    APPLICATION_UPDATES,
    INTERVIEW_INVITATIONS,
    CODING_ASSESSMENTS,
    BANKING_AND_PAYMENTS,
    SECURITY_ALERTS,
    COLLEGE_AND_ACADEMICS,     // DAIICT / DAU mails, exam notices, etc.

    // ── Priority 2 ──────────────────────────────────────────────────────
    LEARNING_PLATFORMS,
    CERTIFICATIONS,
    CODING_PLATFORMS,
    HACKATHONS,
    OPEN_SOURCE,

    // ── Priority 3 ──────────────────────────────────────────────────────
    BLOGS,
    NEWSLETTERS,
    NEWS_FEEDS,
    VIDEO_NOTIFICATIONS,
    WEEKLY_DIGESTS,
    COMMUNITY_UPDATES,

    // ── Priority 4 ──────────────────────────────────────────────────────
    PROMOTIONS,
    MARKETING,
    SPAM,
    GENERAL_UNIVERSITY,

    // ── Priority 5 ──────────────────────────────────────────────────────
    COMMUNITY_ACTIVITIES,      // Dance Club, Music Club, Drama Club, etc.
    EVENT_INVITATIONS
}
