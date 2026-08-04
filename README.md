# PulsePay

> Uzbekistan P2P · C2B · B2B/B2C payment platform

Full-stack monorepo — **Spring Boot** back-end in `server/`, **Vue 3** front-end in `client/`.

```
pulsepay/
├── client/              # Vue 3 + Vite front-end
│   └── src/
│       ├── components/
│       ├── views/
│       ├── stores/
│       ├── router/
│       └── assets/
├── server/              # Spring Boot 4.1.0 back-end
│   ├── src/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   └── docker-compose.yaml
├── .husky/              # Git hooks (pre-commit lint)
├── package.json         # Root orchestration (concurrently)
└── README.md
```

---

## Quick start

### Prerequisites
| Tool | Version |
|------|---------|
| Node | ≥ 22 |
| npm  | ≥ 10 |
| JDK  | 26 (targets Java 25 bytecode) |
| PostgreSQL | 16 (or via Docker) |

### 1 — Install dependencies
```bash
# Root (concurrently + husky)
npm install

# Vue front-end
npm install --prefix client
```

### 2 — Start the back-end
```bash
cd server
docker-compose up -d        # start PostgreSQL
./gradlew bootRun           # start Spring Boot on :8080
```

### 3 — Start the front-end
```bash
npm run client:dev          # Vite dev server on http://localhost:5173
```

---

## Root npm scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start client dev server (extend to add server when ready) |
| `npm run client:dev` | Vite HMR dev server |
| `npm run client:build` | Production build → `client/dist/` |
| `npm run client:lint` | ESLint auto-fix |
| `npm run client:format` | Prettier format |
| `npm run client:test` | Vitest unit tests |
| `npm run client:type-check` | `vue-tsc --noEmit` |

---

## Architecture

### Front-end (`client/`)
| Layer | Library |
|-------|---------|
| Framework | Vue 3 + `<script setup>` Composition API |
| Build | Vite 8 |
| Language | TypeScript (strict mode) |
| Routing | Vue Router 4 (lazy-loaded routes) |
| State | Pinia (setup-store syntax) |
| Server state | TanStack Vue Query |
| Styling | Tailwind CSS v4 |
| Linting | ESLint v10 (flat config) + Prettier |
| Pre-commit | Husky + lint-staged |
| Testing | Vitest + @vue/test-utils |

### Back-end (`server/`)
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.1.0 |
| Language | Java 25 |
| Database | PostgreSQL 16 |
| Migrations | Liquibase (64 changesets) |
| Auth | JWT (jjwt 0.12.6) |
| API Docs | OpenAPI 3 / Swagger UI |
| Architecture | Hexagonal (Ports & Adapters) |
