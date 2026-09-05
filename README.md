# MailInsight — Gmail Inbox Analyzer with Priority-Based Categorization

MailInsight is a full-stack application that connects to a user's Gmail account(s) via OAuth2, analyzes unread/unprocessed emails using the Gemini API, and organizes them into a priority-based dashboard. The goal is to cut through inbox noise by automatically surfacing what actually needs attention (interview invites, security alerts, payment notices) and pushing everything else (newsletters, promotions, social updates) further down.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [System Flow](#system-flow)
- [Core Backend Subsystems](#core-backend-subsystems)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Scope and Limitations](#scope-and-limitations)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)

---

## Overview

A user signs in with Google, links one or more Gmail accounts, and provides their own Gemini API key. When they trigger an analysis, the backend fetches unprocessed emails, batches them into a single structured request (20 emails) per batch, and asks Gemini to classify each email into a category and priority tier with a short summary. Results are validated and persisted; the frontend renders them as charts and a searchable, filterable table.

The project is a modular monolith — a single Spring Boot backend with clearly separated layers (controller, service, repository), rather than microservices. This was a deliberate choice given the scope of the project.

---

## Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security 6, Spring Security OAuth2 Client |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL, HikariCP |
| Gmail Integration | Google API Client Library (`google-api-client`, `google-api-services-gmail`) |
| AI Integration | Gemini API (`gemini-2.0-flash`) via `RestTemplate`, with per-user dynamic API key injection |
| JSON Handling | Jackson `ObjectMapper` |
| Encryption | AES-256-GCM (`javax.crypto`) for API key storage |
| Utilities | Lombok, SLF4J + Logback |
| Build Tool | Maven |

### Frontend
React 18, Vite, React Router, Recharts for charts, native `fetch` with credentialed (cookie-based) requests. Kept intentionally simple — the focus of the project is the backend data logic and API design rather than frontend.

---

## System Flow

```
Browser
   │  1. Google OAuth2 login
   ▼
Spring Security OAuth2 Client ──▶ Google Auth Server
   │  creates/updates User, issues session cookie
   ▼
User adds Gemini API key ──▶ encrypted (AES-256-GCM) ──▶ stored in Postgres
   │
   │  2. User clicks "Analyze Emails"
   ▼
EmailService
   │  fetch unprocessed emails via Gmail API (incremental, by message ID)
   │  threshold check: at least 10 new emails?
   │      no  → return 422, frontend shows progress toward threshold
   │      yes → continue
   │  split emails into batches of 20
   ▼
GeminiService
   │  decrypt user's API key in memory (never sent to frontend)
   │  call Gemini per batch with a fixed JSON schema prompt
   │  parse + validate response (structure, category, priority)
   ▼
Postgres
   │  persist category, priority, summary; mark emails as processed
   ▼
Dashboard (charts, table, category drill-down)
```

## Core Backend Subsystems

**Incremental sync.** The backend tracks which Gmail message IDs have already been processed and only fetches new ones on each run, instead of re-pulling the whole inbox every time.

**Batching instead of per-email calls.** Emails are grouped and sent to Gemini in batches (20 per request) rather than one API call per email. This keeps the number of AI calls proportional to *batches*, not to inbox size, and avoids hitting per-minute rate limits.

**Minimum batch threshold.** Analysis only runs once there are at least 10 unprocessed emails. Below that, the endpoint returns a 422 with the current count so the frontend can show "X of 10 collected" instead of firing a near-empty, low-value AI request.

**Response validation.** Gemini's output is never persisted blindly. Each response is checked for valid JSON structure, and each email's assigned category and priority are checked against the fixed set of allowed values before being written to the database. Malformed or partial responses are handled without failing the whole batch.

**Bring-your-own-key model.** Each user supplies their own Gemini API key rather than the app using a shared/billed key. Keys are encrypted with AES-256-GCM using a server-side secret and only decrypted in memory for the duration of an API call.

**Multi-account support.** A user can link more than one Gmail account (e.g. personal and work/college). Each linked account keeps its own OAuth access/refresh tokens in a separate table, and emails are tagged with which source account they came from so the dashboard can distinguish them.

**Classification taxonomy.** Emails are classified into a fixed set of categories (grouped by domain — career opportunities, interviews, security alerts, banking, learning platforms, newsletters, promotions, campus/community updates, etc.) and one of five priority tiers, from time-sensitive/high-priority down to low-priority/promotional content. The taxonomy is fixed in the service layer rather than user-configurable, to keep classification consistent.

---

## Database Schema

| Table | Purpose |
|---|---|
| `users` | Core user record created/updated on OAuth login |
| `connected_accounts` | Linked Gmail accounts per user, with their own OAuth tokens |
| `emails` | Metadata for each analyzed email (sender, subject, category, priority, summary, timestamps) |
| Gemini key storage | Encrypted per-user API key, tied to `users` |

Only email metadata is stored — sender, subject, AI-generated summary, category, priority, and timestamps. Raw email bodies and attachments are never persisted; they are read from Gmail, sent to Gemini for that single request, and discarded.

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/auth/me` | Returns the logged-in user, or 401 |
| GET | `/api/auth/logout` | Invalidates the session |
| GET | `/api/emails` | Paginated list of analyzed emails |
| GET | `/api/emails/new/count` | Count of unprocessed emails |
| GET | `/api/emails/stats` | Aggregate stats by category and priority |
| GET | `/api/emails/category/{category}` | Paginated emails for a given category |
| POST | `/api/emails/analyze` | Triggers the fetch + Gemini analysis pipeline |
| GET | `/api/user/api-key/status` | Whether a Gemini key is configured |
| POST | `/api/user/api-key` | Save or update the Gemini API key |
| DELETE | `/api/user/api-key` | Remove the stored API key |
| GET | `/api/accounts` | List linked Gmail accounts |
| GET | `/api/accounts/connect` | Get the OAuth URL to link another account |
| DELETE | `/api/accounts/{id}` | Unlink a Gmail account |

---

## Security

- Login is Google OAuth2 only — no passwords are stored.
- Gmail access is read-only (`gmail.readonly` scope); the app cannot send, delete, or modify emails or mailbox labels.
- Gemini API keys are encrypted at rest (AES-256-GCM) and decrypted only in memory, only for the duration of a request.
- Sessions are cookie-based (`JSESSIONID`), invalidated on logout.
- Secrets (DB credentials, OAuth client secret, encryption key) are read from environment variables, never hardcoded.
- Errors are handled through a global exception handler so internal stack traces, tokens, and API keys are never exposed in API responses.

---

## Scope and Limitations

To keep the project focused, the following are intentionally out of scope:

- No sending, replying to, deleting, or modifying emails — the app is read-only.
- No attachment parsing — only subject, sender, and body text/snippet are used.
- No real-time sync. Analysis is on-demand, triggered by the user.
- No team/multi-tenant workspaces — each account is single-user.
- No offline mode or native mobile app.

---

## Running Locally

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL
- A Google Cloud project with OAuth2 credentials and the Gmail API enabled
- A Gemini API key from Google AI Studio

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

---

## Environment Variables

### Backend
| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `GOOGLE_CLIENT_ID` | OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth2 client secret |
| `ENCRYPTION_SECRET` | Secret used to derive the AES-256 key for encrypting stored Gemini API keys |
| `FRONTEND_ORIGIN` | Allowed CORS origin |

### Frontend
| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Backend base URL |