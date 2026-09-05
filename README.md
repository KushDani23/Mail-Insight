# MailInsight — AI-Powered Email Intelligence

> Analyze your Gmail inbox with Google Gemini AI. Get smart categorization, priority detection, and plain-English summaries — all in one dashboard.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [User Flow](#user-flow)
- [Backend Architecture](#backend-architecture)
- [API Endpoints](#api-endpoints)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)

---

## Overview

MailInsight connects to your Gmail account via OAuth2, fetches your emails, and runs them through the **Google Gemini AI** to produce:

- **Categorization** — Work, Personal, Finance, Promotions, Social, Updates, Spam, Other
- **Priority Detection** — High, Medium, Low
- **AI Summaries** — A short plain-English summary of what each email is about

All of this is displayed in an interactive dashboard with charts, a searchable email table, and per-email detail views.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL (Neon for cloud) |
| **Auth** | Google OAuth2 (via Spring Security OAuth2 Client) |
| **AI Integration** | Google Gemini API (via `RestTemplate` — no Spring AI dependency) |
| **Email Fetching** | Gmail API (via Google API Client Library) |
| **Security** | AES-256 encryption for storing user Gemini API keys |
| **Frontend** | React 18, Vite, React Router v7, Recharts |

---

## User Flow

This section describes exactly what happens from the moment a user visits the app to the moment they log out.

### Step 1 — Visit the App
The user arrives at the landing/login page. They are **not authenticated**, so the app shows the sign-in screen. No data is loaded at this point.

### Step 2 — Google Sign-In (OAuth2)
The user clicks **"Sign in with Google"**. This redirects them to Google's OAuth2 consent screen, where they authorize the app to:
- View their **Google account profile** (name, email, profile picture)
- **Read** their Gmail messages and metadata

> **What happens in the backend:**
> Spring Security's OAuth2 client handles the redirect. After Google returns an authorization code, the backend exchanges it for an access token and refresh token. A new `User` record is created (or the existing one is updated) in the database with the Gmail address and OAuth tokens stored. A session (`JSESSIONID` cookie) is then issued to the browser.

### Step 3 — Dashboard (First Time)
The user lands on the **Overview** dashboard. At this point:
- Stats are shown (total emails may be zero if first time).
- The **"Gemini API Key"** status in the Settings card shows **"Not Set"**.
- The **Category and Priority charts** are empty.

The user cannot run AI analysis yet because no Gemini API key is configured.

### Step 4 — Add Gemini API Key
The user navigates to **Settings** and clicks **"Add Key"**. A modal appears where they paste their personal Gemini API key (obtained for free from [Google AI Studio](https://aistudio.google.com/app/apikey)).

> **What happens in the backend:**
> The key is encrypted with **AES-256** using an `ENCRYPTION_SECRET` known only to the server, then stored in the `users` table. The raw key is never persisted anywhere. On every subsequent AI call, the backend decrypts the key in-memory, uses it to make the Gemini API request, and discards it immediately.

### Step 5 — (Optional) Connect Additional Gmail Accounts
From Settings, the user can link secondary Gmail accounts. This triggers another Google OAuth2 flow for the additional account. Connected accounts are stored in the `connected_accounts` table, each with their own access and refresh tokens.

> This allows MailInsight to aggregate and analyze emails from **multiple Gmail inboxes** into one unified dashboard.

### Step 6 — Analyze Emails
The user clicks the **"Analyze Emails"** button in the top header.

> **What happens in the backend (full flow):**
> 1. The backend calls the Gmail API using the stored OAuth tokens to fetch emails that have **not yet been analyzed** (tracked by a `isNew` flag on each `Email` entity).
> 2. It checks if there are **at least 10 new emails**. If not, it throws an `InsufficientEmailsException` and returns a `422` response.
> 3. If the threshold is met, it bundles the emails into a structured prompt and calls the **Gemini API** (`gemini-2.0-flash`) using the user's own decrypted API key.
> 4. Gemini returns a JSON response assigning a `category`, `priority`, and `summary` to each email.
> 5. The backend saves these results back to the `emails` table and marks the emails as analyzed (`isNew = false`, `analyzedAt` timestamp set).

> **Why 10 emails minimum?**
> The Gemini API has per-minute token rate limits. Batching ensures users get a complete, meaningful analysis in one shot without hitting quota errors mid-way.

**If < 10 new emails exist**, a modal appears on the frontend showing:
- A progress bar (e.g., "7 / 10 emails collected")
- A message explaining the rate-limit reasoning
- A count of how many more emails are needed

### Step 7 — View Results
Once analysis is complete, the dashboard updates:

- **Overview Page** — The donut chart populates with category percentages; the bar chart shows High/Medium/Low priority counts; summary cards update.
- **Emails Page** — The full email table shows each email with its category badge, priority indicator, and the first line of the AI summary.
- **Email Detail Drawer** — Clicking any row in the email table slides open a detail panel showing the full AI-generated summary, sender, received date, and category/priority badges.

### Step 8 — Logout
The user clicks **"Logout"** in the sidebar. The backend invalidates the HTTP session. The browser is redirected back to the login page and the session cookie is cleared.

---

## Backend Architecture

```
com.mailinsight
├── config/          # SecurityConfig, CORS, session settings
├── controller/      # REST controllers (Auth, Email, User, Account)
├── dto/             # Request/Response data transfer objects
├── entity/          # JPA entities: User, Email, ConnectedAccount
├── enums/           # EmailCategory, EmailPriority
├── exception/       # InsufficientEmailsException, GlobalExceptionHandler
├── oauth2/          # Custom OAuth2 success handler
├── repository/      # Spring Data JPA repositories
├── service/         # GmailService, GeminiService, EmailService, UserService
└── util/            # AES encryption utility
```

### Key Design Decisions

- **Per-user Gemini keys via `RestTemplate`:** Spring AI was intentionally avoided because it doesn't support injecting dynamic API keys per-request. Using `RestTemplate` directly gives full control over the `Authorization` header per user.
- **AES-256 Encryption:** User Gemini keys are never stored in plaintext. The `ENCRYPTION_SECRET` env variable is the only key needed to decrypt them.
- **Token Refresh:** Gmail OAuth tokens are refreshed automatically before each Gmail API call if they are close to expiry.

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/auth/me` | Returns the currently logged-in user (or 401) |
| `GET` | `/api/auth/logout` | Invalidates session and logs out |
| `GET` | `/api/emails` | Paginated list of all emails for the user |
| `GET` | `/api/emails/new/count` | Count of unanalyzed emails |
| `GET` | `/api/emails/stats` | Aggregate stats (totals, by category, by priority) |
| `GET` | `/api/emails/category/{cat}` | Paginated emails filtered by category |
| `POST` | `/api/emails/analyze` | Triggers Gmail fetch + Gemini AI analysis |
| `GET` | `/api/user/api-key/status` | Whether the user has a Gemini API key set |
| `POST` | `/api/user/api-key` | Save or update Gemini API key |
| `DELETE` | `/api/user/api-key` | Remove Gemini API key |
| `GET` | `/api/accounts` | List all connected Gmail accounts |
| `GET` | `/api/accounts/connect` | Get Google OAuth2 URL to connect another account |
| `DELETE` | `/api/accounts/{id}` | Disconnect a linked Gmail account |

---

## Running Locally

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL database (local or [Neon](https://neon.tech) free tier)
- Google Cloud project with OAuth2 credentials and Gmail API enabled
- Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

### 1. Backend

```bash
cd backend

# Create application-local.properties (see Environment Variables section)
./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`.

---

## Environment Variables

### Backend (`backend/src/main/resources/application.properties` or env)

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL (e.g. `jdbc:postgresql://...`) |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `GOOGLE_CLIENT_ID` | OAuth2 Client ID from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Client Secret |
| `ENCRYPTION_SECRET` | Random 32-char string for AES key encryption (`openssl rand -base64 32`) |
| `FRONTEND_ORIGIN` | Allowed CORS origin (e.g. `http://localhost:5173` locally) |

### Frontend (`.env` file in `frontend/`)

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Backend base URL (e.g. `http://localhost:8080`) |

---

> Built with ❤️ using Spring Boot + Gemini AI
