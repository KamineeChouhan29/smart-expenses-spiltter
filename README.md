# Smart Expense Splitter
> **Neev Cloud Internship Assignment** — Full-Stack Web Application

A lightweight, mobile-first web app for tracking shared group expenses with **AI-powered categorization** and smart spending insights.

---

## Features

| Feature | Details |
|---|---|
| 🔐 Auth | JWT-based Registration & Login |
| 👥 Groups | Create groups, add members |
| 💸 Expenses | Add expenses in INR (₹) |
| ⚖️ Split Logic | Equal split & Custom (by ₹ or %) |
| 📊 Balances | Real-time "Who Owes Whom" |
| 🤖 AI Category | Groq Llama 3 auto-categorizes expenses |
| ✨ AI Insights | Weekly spending pattern analysis |
| 📱 Mobile-first | Optimised for 390px (Realme C35) viewport |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17+ (Standalone), Tailwind CSS |
| Backend | Java Spring Boot 3.2, Spring Security |
| Auth | JWT (JJWT 0.12.x) |
| Database | MySQL 8 |
| AI | Groq API (llama3-70b-8192) |
| HTTP Client | Spring WebFlux WebClient |

---

## Project Structure

```
smart-expenses-splitter/
├── backend/                          # Spring Boot 3.x
│   ├── pom.xml
│   └── src/main/java/com/neev/expensesplitter/
│       ├── config/SecurityConfig.java
│       ├── controller/               # Auth, Group, Expense
│       ├── dto/                      # Request/Response records
│       ├── exception/GlobalExceptionHandler.java
│       ├── model/                    # JPA Entities
│       ├── repository/               # Spring Data JPA repos
│       ├── security/                 # JWT Provider, Filter, UDS
│       └── service/                  # Auth, Group, Expense, AI
├── frontend/                         # Angular 17+ (standalone)
│   └── src/app/
│       ├── auth/                     # Login, Register
│       ├── core/
│       │   ├── guards/auth.guard.ts
│       │   ├── interceptors/jwt.interceptor.ts
│       │   ├── models/models.ts
│       │   └── services/             # Auth, Group, Expense
│       ├── dashboard/
│       ├── expenses/add-expense/
│       ├── groups/group-detail/
│       └── shared/navbar/
├── schema.sql
└── README.md
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+, npm
- MySQL 8
- Groq API Key → https://console.groq.com/

---

## Setup Instructions

### 1 — Database

```sql
-- Run schema.sql in your MySQL client:
mysql -u root -p < schema.sql
```

Or let Spring Boot auto-create tables via `spring.jpa.hibernate.ddl-auto=update` (already configured).

### 2 — Backend

**Set your credentials** in `backend/src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=<YOUR_MYSQL_PASSWORD>

groq.api.key=<YOUR_GROQ_API_KEY>
```

**Run:**
```bash
cd backend
mvn spring-boot:run
```
Backend starts at **http://localhost:8080**

### 3 — Frontend

```bash
cd frontend
npm install
npm start          # or: npx ng serve
```
Frontend starts at **http://localhost:4200**

---

## API Reference

### Auth (Public)
| Method | URL | Body |
|---|---|---|
| POST | `/api/auth/register` | `{username, email, password}` |
| POST | `/api/auth/login` | `{email, password}` |

### Groups (JWT required)
| Method | URL | Description |
|---|---|---|
| GET | `/api/groups` | List my groups |
| POST | `/api/groups` | Create group |
| POST | `/api/groups/{id}/members` | Add member by username |
| GET | `/api/groups/{id}/members` | List members |
| GET | `/api/groups/{id}/balances` | Who owes whom |

### Expenses (JWT required)
| Method | URL | Description |
|---|---|---|
| POST | `/api/expenses` | Add expense (triggers AI) |
| GET | `/api/expenses/group/{id}` | List group expenses |
| GET | `/api/expenses/insights` | AI weekly insights |

---

## Split Logic

### Equal Split
Amount is divided equally among all group members: `amount / members.count` (rounded to 2 decimal places).

### Custom Split
- **By Amount**: Each member's `amountOwed` is specified explicitly. Must sum to total expense.
- **By Percentage**: Each member has a percentage share. Must sum to 100%.

---

## AI Integration (Groq)

### Categorization Prompt
> *"You are an expense categorization assistant. Given an expense description, respond ONLY with a valid JSON object. Example: `{"category": "Food"}`. The category must be exactly one of: Food, Travel, Rent, Shopping, Other."*

### Insights Prompt
> *"You are a financial insights assistant working in INR. Given weekly expense data grouped by category, respond ONLY with a valid JSON object like: `{"insights": ["insight1", "insight2"]}`. Provide 2-3 short, actionable insights comparing current vs previous week spending."*

---

## Balance Calculation Algorithm

For each group:
1. Loop through all expense splits
2. For splits where `split.user ≠ expense.payer` → build `net[debtor][creditor] += amountOwed`
3. For each unique pair (A, B): `net = A-owes-B − B-owes-A`
4. If net > 0 → **A owes B** the net amount
5. Returns simplified list of `{debtor, creditor, amount}`

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────┐
│                ANGULAR FRONTEND                  │
│   AuthGuard → JwtInterceptor → Components       │
│   (Login | Register | Dashboard | GroupDetail)   │
└─────────────┬───────────────────────────────────┘
              │ HTTP + JWT Bearer
┌─────────────▼───────────────────────────────────┐
│            SPRING BOOT BACKEND                   │
│  JwtAuthFilter → Controllers → Services          │
│  ┌──────────┬──────────┬──────────┬───────────┐ │
│  │AuthCtrl  │GroupCtrl │ExpenseCtrl│AIService  │ │
│  └──────────┴──────────┴─────┬────┴───────────┘ │
│                               │ Groq API (HTTPS) │
└─────────────┬─────────────────┘─────────────────┘
              │ JPA
┌─────────────▼────────────┐
│         MySQL 8           │
│ users | groups | members  │
│ expenses | splits         │
└───────────────────────────┘
```

---

## Evaluation Criteria Coverage

| Criterion | Status |
|---|---|
| Authentication (JWT) | ✅ |
| Group Management | ✅ |
| Equal Split | ✅ |
| Custom Split (amount + %) | ✅ |
| AI Categorization (Groq) | ✅ |
| AI Insights (weekly patterns) | ✅ |
| Currency in INR (₹) | ✅ |
| Mobile-responsive UI | ✅ |
| Real-time balance updates | ✅ (RxJS signals) |
| Clean JSON prompt engineering | ✅ |
| schema.sql | ✅ |
| README | ✅ |
