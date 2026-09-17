<p align="center">
  <img src="https://img.shields.io/badge/Status-Live-brightgreen?style=for-the-badge" alt="Status" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React" />
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Qdrant-Vector%20DB-DC382D?style=for-the-badge" alt="Qdrant" />
  <img src="https://img.shields.io/badge/Vercel-Deployed-000?style=for-the-badge&logo=vercel&logoColor=white" alt="Vercel" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

# 🔵 SkillCircle

### AI-Driven Developer Collaboration Platform

> *Find your perfect project partner in minutes, not days. SkillCircle understands what you know, what you want to build, and who you work best with.*

<p align="center">
  <a href="https://skillcircle.laveshgaur.com"><strong>🌐 Live Demo — skillcircle.laveshgaur.com</strong></a>
</p>

---

## 🧠 What is SkillCircle?

**SkillCircle** is an intelligent, full-stack developer community platform that uses a **two-stage AI matching pipeline** to recommend ideal collaborators for open-source projects and hackathons.

Unlike keyword-based platforms (DevPost, LinkedIn), SkillCircle captures the **semantic richness** of a developer's skills, goals, and collaboration history — delivering matches that are measurably more accurate.

---

## 🔍 The Problem

| Pain Point | Impact |
|-----------|--------|
| Keyword search is semantically shallow | "React developer" misses "frontend engineer with component architectures" |
| No collaboration history signal | Platforms ignore who has successfully worked together before |
| Cold-start problem for newcomers | New developers with no history get zero recommendations |
| Timezone & availability blindness | Matches ignore whether people can work synchronously |
| Goal misalignment | A learner gets matched with someone needing a production engineer |
| Fragmented tooling | Matching on one platform, chat on another, tasks on a third |

---

## ⚙️ How It Works — Two-Stage Matching Pipeline

```
┌──────────────────┐     ┌────────────────────┐     ┌──────────────────┐
│   Stage 1:       │     │    Stage 2:         │     │   Stage 3:       │
│   Semantic       │────►│    Collaborative    │────►│   Hard Filters   │
│   Retrieval      │     │    Re-Ranking       │     │   (Post-process) │
└──────────────────┘     └────────────────────┘     └──────────────────┘
       │                          │                          │
  LLM Embeddings +         GitHub co-contrib          Timezone overlap,
  Vector DB ANN            graph + history            availability,
  (top-K = 50)             weighted re-score          goal alignment
```

### Stage 1 — Semantic Retrieval
Developer profiles (skills + bio + goals) are encoded into **high-dimensional embedding vectors** using LLMs. Approximate Nearest Neighbor (ANN) search over a vector database retrieves the top-50 semantically similar candidates.

### Stage 2 — Collaborative Filtering Re-Ranker
A **co-contribution graph** built from GitHub data (shared repos, PRs, issues) provides collaboration signals. Candidates are re-scored using a tunable weighted function:

```
final_score = α × semantic_similarity + β × collab_score + γ × goal_alignment
```
*(where α + β + γ = 1.0, configurable per deployment)*

For **cold-start users** (no collaboration history), β → 0 and the system gracefully falls back to semantic + goal matching.

### Stage 3 — Hard Filters
Boolean post-filters on timezone overlap, availability status, and goal type ensure practical viability of every recommendation.

---

## 🏗️ Platform Features

### 🤝 Intelligent Matching
- Semantic profile embeddings via OpenAI text-embedding-3-small
- ANN search over Qdrant vector database
- GitHub-powered collaborative filtering with co-contribution graph
- Cold-start handling with graceful degradation
- Accept/dismiss match actions with animated transitions
- Match history tracking and review
- Animated SVG score rings with breakdown (semantic/collab/goal)

### 💬 Real-Time Community Spaces
- WebSocket-powered live chat (STOMP via `@stomp/stompjs`)
- 3-panel layout (spaces → threads → messages)
- Threaded discussions with typing indicators (animated dots)
- Online presence tracking (Redis sorted set)
- Space creation, thread pinning, message compose bar

### 🤖 AI-Powered Insights
- Automatic thread summarization (GPT-4o-mini with heuristic fallback)
- Skill extraction from GitHub repositories (LLM → catalog → regex chain)
- Smart profile enrichment via repo README parsing
- Skill autocomplete with debounced search

### 📋 Project Task Boards
- Drag-and-drop Kanban board (`@dnd-kit` integration)
- Task detail modal with status transitions and assignment
- Team member management with role hierarchy
- Project-linked community spaces (auto-created)
- Priority color coding (URGENT/HIGH/MEDIUM/LOW)

### 👤 Developer Profiles
- Rich profile editor with skill autocomplete
- Profile completeness tracking (8-field score + progress bar)
- GitHub sync for automatic skill extraction
- LinkedIn & website URL integration
- Availability status (OPEN/SELECTIVE/BUSY) with online presence

### 🔐 Secure Authentication
- OAuth 2.0 (GitHub + Google) with branded buttons
- JWT with refresh token rotation (Redis-backed)
- Role-based access control (USER, ADMIN)
- Protected routes with automatic redirect

### 🎨 Modern Design System
- CSS custom properties with 200+ design tokens
- Dark/light theme toggle with system preference detection
- Glassmorphism effects and smooth gradients
- Responsive design (desktop → tablet → mobile)
- Toast notifications, modals, spinners, error boundaries

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------:|
| **Frontend** | React 19, Vite 8, React Router 7, Zustand 5, Axios |
| **UI Kit** | CSS Modules, custom tokens.css design system, Lucide icons |
| **Real-Time (FE)** | @stomp/stompjs (WebSocket) |
| **Drag & Drop** | @dnd-kit/core, @dnd-kit/sortable |
| **Backend** | Spring Boot 4.1, Java 21, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 15+ |
| **Cache** | Redis 7+ |
| **Vector DB** | Qdrant (ANN search for embeddings) |
| **AI / Embeddings** | OpenAI text-embedding-3-small, GPT-4o-mini |
| **Real-Time (BE)** | Spring WebSocket (STOMP + SockJS) |
| **Auth** | OAuth 2.0 (GitHub + Google) + JWT (access + refresh rotation) |
| **Deployment** | Vercel (frontend), VPS (backend), Nginx (reverse proxy) |

---

## 📊 Evaluation Metrics

The matching pipeline is evaluated using standard information retrieval metrics:

| Metric | Description | Target |
|--------|-------------|--------|
| **Precision@5** | Fraction of top-5 recommendations accepted by user | ≥ 0.70 |
| **NDCG@10** | Normalized ranking quality of top-10 results | ≥ 0.75 |
| **Match-to-Collab Rate** | % of accepted matches leading to real projects | ≥ 30% |

Benchmarks compare hybrid (semantic + collab) against single-stage baselines across cold-start and warm-start user cohorts.

---

## 🏛️ Architecture Overview

```
                    ┌─────────────────────┐
                    │   Vercel CDN        │
                    │   (Frontend SPA)    │
                    │   skillcircle.      │
                    │   laveshgaur.com    │
                    └──────────┬──────────┘
                               │ API calls
                               ▼
                    ┌─────────────────────┐
                    │   VPS + Nginx       │
                    │   (Reverse Proxy)   │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Spring Boot API   │
                    │   + WebSocket       │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                     │
┌─────────▼────────┐  ┌───────▼───────┐  ┌─────────▼────────┐
│   PostgreSQL     │  │    Qdrant     │  │     Redis        │
│   (Primary DB)   │  │  (Vector DB)  │  │   (Cache/JWT)    │
└──────────────────┘  └───────────────┘  └──────────────────┘
```

---

## 🌐 Deployment

| Component | Platform | URL |
|-----------|----------|-----|
| **Frontend** | Vercel | [skillcircle.laveshgaur.com](https://skillcircle.laveshgaur.com) |
| **Backend API** | VPS | Proxied via Nginx |
| **Database** | VPS | PostgreSQL 15+ |
| **Cache** | VPS | Redis 7+ |
| **Vector DB** | VPS | Qdrant |

### Vercel Environment Variables

Set these in **Vercel Dashboard → Settings → Environment Variables**:

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Backend API URL (e.g., `https://api.yourdomain.com/api/v1`) |

### Backend Environment Variables

Copy `Backend/.env.example` to `Backend/.env` and configure all values. See the [.env.example](Backend/.env.example) for the full list.

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL 15+
- Redis 7+
- Qdrant (optional — matching works without it via graceful fallback)

### Backend Setup

```bash
cd Backend
cp .env.example .env
# Edit .env with your database, Redis, OAuth, and OpenAI credentials
mvn spring-boot:run
```

The backend starts at `http://localhost:8080` with Swagger UI at `/swagger-ui.html`.

### Frontend Setup

```bash
cd Frontend
cp .env.example .env
# Edit .env if needed (defaults to http://localhost:8080/api/v1)
npm install
npm run dev
```

The frontend starts at `http://localhost:5173`.

### Environment Variables

#### Backend (`Backend/.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8080` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/skillcircle` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | — |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | Base64-encoded 256-bit key (`openssl rand -base64 32`) | — |
| `JWT_ACCESS_EXPIRY` | Access token TTL (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRY` | Refresh token TTL (ms) | `604800000` (7 days) |
| `GITHUB_CLIENT_ID` | GitHub OAuth app client ID | — |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app client secret | — |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | — |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | — |
| `CORS_ORIGINS` | Allowed frontend origins (comma-separated) | `http://localhost:5173` |
| `OPENAI_API_KEY` | OpenAI API key for embeddings & chat | — |
| `OPENAI_EMBEDDING_MODEL` | Embedding model | `text-embedding-3-small` |
| `QDRANT_URL` | Qdrant server URL | `http://localhost:6333` |
| `QDRANT_COLLECTION` | Qdrant collection name | `skillcircle_profiles` |

#### Frontend (`Frontend/.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_URL` | Backend API base URL | `http://localhost:8080/api/v1` |

---

## 📁 Project Structure

```
SkillCircle/
├── Frontend/                 # React 19 + Vite 8 SPA (Vercel)
│   ├── src/
│   │   ├── components/
│   │   │   ├── icons/        # Custom brand icons (GitHub, Google)
│   │   │   ├── layout/       # AppLayout, Navbar, Sidebar, PageWrapper, ErrorBoundary
│   │   │   └── ui/           # Button, Input, Modal, Avatar, Badge, Card, Spinner,
│   │   │                     # Select, Tabs, ProgressBar, EmptyState, Toast
│   │   ├── lib/              # API client (Axios + JWT), WebSocket client, utilities
│   │   ├── pages/
│   │   │   ├── auth/         # Login, Register, OAuth Callback
│   │   │   ├── community/    # 3-panel chat (spaces/threads/messages)
│   │   │   ├── matches/      # Match Explorer (pipeline, filters, history)
│   │   │   ├── profile/      # Profile editor with skill autocomplete
│   │   │   ├── projects/     # Kanban board with drag-and-drop
│   │   │   ├── settings/     # Account preferences
│   │   │   ├── Dashboard.jsx
│   │   │   └── Landing.jsx
│   │   ├── routes/           # ProtectedRoute, PublicOnlyRoute
│   │   ├── services/         # API modules (auth, profile, match, community, project)
│   │   ├── store/            # Zustand stores (auth, UI)
│   │   └── styles/           # Design tokens (tokens.css) + global styles
│   ├── .env.example          # Frontend env template
│   ├── vercel.json           # SPA rewrite rules for Vercel
│   └── package.json
│
├── Backend/                  # Spring Boot 4.1 API (VPS)
│   ├── src/main/java/com/skillcircle/
│   │   ├── auth/             # OAuth2 + JWT authentication (20 tests)
│   │   ├── profile/          # User profile management
│   │   ├── matching/         # Core matching engine — 3-stage pipeline (17 tests)
│   │   ├── community/        # Spaces, threads, WebSocket chat (18 tests)
│   │   ├── project/          # Project boards & task management (12 tests)
│   │   ├── ai/               # Summarization & skill extraction (20 tests)
│   │   ├── config/           # Security, CORS, WebSocket, Redis configs
│   │   ├── common/           # Shared utilities and base classes
│   │   └── exception/        # Global exception handling
│   ├── src/main/resources/
│   │   └── application.properties  # Config (reads from .env)
│   ├── .env.example          # Backend env template
│   └── pom.xml
│
├── .planning/                # Architecture & design documents
│   ├── 01_SRS.md             # Software Requirements Specification
│   ├── 02_HLD.md             # High-Level Design
│   ├── 03_LLD.md             # Low-Level Design
│   └── 04_WORKFLOW_PLAN.md   # Development workflow (Phases 0-10)
│
└── README.md
```

---

## 🗺️ Roadmap

- [x] System design & architecture planning
- [x] Phase 0: Project bootstrap & infrastructure
- [x] Phase 1: Authentication (OAuth2 + JWT) — *20 tests*
- [x] Phase 2: Profile & skill management — *13 tests*
- [x] Phase 3: Matching engine (core AI pipeline) — *17 tests*
- [x] Phase 4: Community spaces & real-time chat — *18 tests*
- [x] Phase 5: Project task boards — *12 tests*
- [x] Phase 6: AI services (summarization, skill extraction) — *20 tests*
- [x] Phase 7: Frontend shell (design system, routing, layout)
- [x] Phase 8: Frontend features (all pages, WebSocket chat, DnD Kanban)
- [x] Phase 9: Evaluation & benchmarking — *39 tests, NDCG@10 = 0.80*
- [x] Phase 10: Production deployment (Vercel + VPS)

**Backend: 139 unit/integration tests** | **Frontend: 6 feature pages, 12 UI components, production build passes**

---

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines before submitting a PR.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit using conventional commits (`feat:`, `fix:`, `docs:`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <b>SkillCircle</b> — Intelligent collaboration starts here.<br/>
  <a href="https://skillcircle.laveshgaur.com">skillcircle.laveshgaur.com</a>
</p>