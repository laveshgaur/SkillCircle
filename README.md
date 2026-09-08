<p align="center">
  <img src="https://img.shields.io/badge/Status-In%20Development-yellow?style=for-the-badge" alt="Status" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React" />
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Qdrant-Vector%20DB-DC382D?style=for-the-badge" alt="Qdrant" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

# 🔵 SkillCircle

### AI-Driven Developer Collaboration Platform

> *Find your perfect project partner in minutes, not days. SkillCircle understands what you know, what you want to build, and who you work best with.*

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
- Semantic profile embeddings via OpenAI / Cohere
- ANN search over Qdrant vector database
- GitHub-powered collaborative filtering
- Cold-start handling with graceful degradation

### 💬 Real-Time Community Spaces
- WebSocket-powered live chat (STOMP + SockJS)
- Threaded discussions with typing indicators
- Online presence tracking

### 🤖 AI-Powered Insights
- Automatic thread summarization (GPT-4o-mini / Gemini Flash)
- Skill extraction from GitHub repositories
- Smart profile enrichment

### 📋 Project Task Boards
- Kanban-style task management
- Team assembly from matched collaborators
- Project-linked community spaces

### 🔐 Secure Authentication
- OAuth 2.0 (GitHub + Google)
- JWT with refresh token rotation
- Role-based access control

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | React 18, Vite, React Router, Zustand, Axios |
| **Backend** | Spring Boot 3.x, Java 17, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 15+ |
| **Cache** | Redis 7+ |
| **Vector DB** | Qdrant (ANN search for embeddings) |
| **AI / Embeddings** | OpenAI text-embedding-3-small, GPT-4o-mini |
| **Real-Time** | Spring WebSocket (STOMP + SockJS) |
| **Message Queue** | RabbitMQ |
| **Auth** | OAuth 2.0 + JWT |
| **Containerization** | Docker + Docker Compose |
| **CI/CD** | GitHub Actions |

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
                         ┌──────────────┐
                         │    Nginx     │
                         │  (Reverse    │
                         │   Proxy)     │
                         └──────┬───────┘
                                │
                   ┌────────────┴────────────┐
                   │                         │
          ┌────────▼────────┐      ┌────────▼────────┐
          │   React SPA     │      │  Spring Boot    │
          │   (Vite)        │      │  API + WebSocket│
          └─────────────────┘      └───────┬─────────┘
                                           │
                    ┌──────────────────────┬┴──────────────┐
                    │                      │               │
           ┌────────▼──────┐    ┌─────────▼───┐   ┌──────▼──────┐
           │  PostgreSQL   │    │   Qdrant    │   │    Redis    │
           │  (Primary DB) │    │ (Vector DB) │   │  (Cache)    │
           └───────────────┘    └─────────────┘   └─────────────┘
                    │
           ┌────────▼──────┐
           │   RabbitMQ    │──► Embedding & Notification Workers
           └───────────────┘
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose

### Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/SkillCircle.git
cd SkillCircle

# Start infrastructure services
docker-compose up -d

# Backend
cd Backend
cp ../.env.example .env
./mvnw spring-boot:run

# Frontend (new terminal)
cd Frontend
npm install
npm run dev
```

### Environment Variables

Copy `.env.example` and configure:
- `OPENAI_API_KEY` — for embedding generation
- `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` — for OAuth
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — for OAuth
- Database, Redis, Qdrant connection details

---

## 📁 Project Structure

```
SkillCircle/
├── Frontend/               # React 18 + Vite SPA
│   ├── src/
│   │   ├── components/     # Reusable UI (common, layout, match, community, project)
│   │   ├── pages/          # Route-level pages
│   │   ├── hooks/          # Custom React hooks
│   │   ├── services/       # API client modules
│   │   ├── store/          # Zustand state management
│   │   └── styles/         # Design system & tokens
│   └── package.json
│
├── Backend/                # Spring Boot 3.x API
│   ├── src/main/java/com/skillcircle/
│   │   ├── auth/           # OAuth2 + JWT authentication
│   │   ├── user/           # User & profile management
│   │   ├── matching/       # Core matching engine (3-stage pipeline)
│   │   ├── community/      # Spaces, threads, WebSocket chat
│   │   ├── project/        # Project boards & task management
│   │   ├── ai/             # Summarization & skill extraction
│   │   ├── notification/   # Event-driven notifications
│   │   └── config/         # Security, CORS, WebSocket, Redis configs
│   └── pom.xml
│
├── docker-compose.yml      # PostgreSQL, Redis, Qdrant, RabbitMQ
└── .env.example
```

---

## 🗺️ Roadmap

- [x] System design & architecture planning
- [x] Phase 0: Project bootstrap & infrastructure
- [x] Phase 1: Authentication (OAuth2 + JWT)
- [x] Phase 2: Profile & skill management
- [x] Phase 3: Matching engine (core AI pipeline)
- [x] Phase 4: Community spaces & real-time chat
- [x] Phase 5: Project task boards
- [x] Phase 6: AI services (summarization, skill extraction)
- [x] Phase 7-8: Frontend implementation
- [ ] Phase 9: Evaluation & benchmarking
- [ ] Phase 10: Production deployment

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
  <b>SkillCircle</b> — Intelligent collaboration starts here.
</p>