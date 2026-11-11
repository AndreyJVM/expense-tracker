# Expense Tracker REST API & Dashboard
![CI Status](https://github.com/felipeaugdev/expense-tracker/actions/workflows/ci.yml/badge.svg)

**Live Demo:** [https://expense-tracker-4d81.onrender.com](https://expense-tracker-4d81.onrender.com)  
*(May take 30–60 seconds to wake up on first visit)*

A full-stack personal expense tracking application with real-time analytics. Built with Java 21, Spring Boot, MariaDB, Docker, and a modern Vanilla JS/Tailwind CSS dashboard.

## Screenshots

### Dashboard (Dark Mode)
![Dashboard Dark](screenshots/dashboard-dark.png)

### Dashboard (Light Mode)
![Dashboard Light](screenshots/dashboard-light.png)

### Authentication Modal
![Auth Modal](screenshots/auth-modal.png)

## Features

- **Web Dashboard:** Interactive single-page dashboard displaying real-time monthly metrics, spending category charts, and a transaction table.
- **Expense Management:** Full CRUD operations for creating, viewing, editing, and deleting expense records.
- **Date Range Filtering:** Filter transactions by rolling time windows (e.g., last 30, 14, or 7 days).
- **Category Breakdown:** Aggregated spending totals grouped by expense category. Also supports custom categories (requires login).
- **Month-over-Month Reports:** Comparative analysis between current and previous calendar month spending.
- **Recurring Expenses:** Mark an expense as recurring (every 30, 14 or 7 days). Future occurrences are generated automatically. A small icon indicates recurring items in the transaction list.
- **Optional User Accounts:** Register / login with JWT authentication. Expenses are isolated per user while guest mode remains fully functional.
- **Monthly Budgets:** Set per-category spending limits, track progress with visual bars, and get warned when a limit is reached (requires login).
- **Dockerized Environment:** Multi-stage Docker build packaging both Spring Boot backend and MariaDB database via Docker Compose.
- **Secure API:** Spring Security + JJWT protect authenticated endpoints while keeping public access for guests.
- **API Documentation:** Interactive Swagger UI generated via SpringDoc OpenAPI.

## Tech Stack

- **Language & Framework:** Java 21 LTS, Spring Boot 3.3 (Spring Web, Spring Data JPA, Spring Security)
- **Authentication:** JWT (JJWT library)
- **Frontend:** HTML5, Tailwind CSS (CDN), Vanilla JavaScript (ES6+), Chart.js
- **Database:** MariaDB 11 (local) / PostgreSQL (live demo on Render)
- **Build & Package:** Maven, Docker, Docker Compose
- **Testing:** JUnit 5, Mockito, MockMvc
- **Documentation:** SpringDoc OpenAPI

## API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET`    | `/api/expenses`          | Fetch all expenses (supports optional `?days=N` query param) |
| `POST`   | `/api/expenses`          | Create a new expense |
| `PUT`    | `/api/expenses/{id}`     | Update an existing expense |
| `DELETE` | `/api/expenses/{id}`     | Delete an expense by ID |
| `GET`    | `/api/expenses/category-totals` | Get spending aggregated by category |
| `GET`    | `/api/expenses/monthly-report` | Get month-over-month report metrics |
| `GET`    | `/api/categories`        | List system + user categories |
| `POST`   | `/api/categories`        | Create a custom category (auth required) |
| `GET`    | `/api/budgets`           | List budgets for the current user |
| `POST`   | `/api/budgets`           | Create or update a budget |
| `DELETE` | `/api/budgets/{id}`      | Delete a budget |
| `POST`   | `/api/recurring/generate` | Manually trigger generation of due recurring expenses (for testing/development) |

### Authentication Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Create a new user account |
| `POST` | `/api/auth/login`    | Obtain a JWT token |

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) or [Docker Engine + Docker Compose](https://docs.docker.com/engine/install/) (Linux).

### Running the Application

1. **Clone the repository:**
   ```bash
   git clone https://github.com/felipeaugdev/expense-tracker.git
   cd expense-tracker
   ```

2. **Start the stack:**
   ```bash
   docker compose up -d
   ```

3. **Access the application:**
   - **Web Dashboard:** `http://localhost:8080/`
   - **Swagger UI:** `http://localhost:8080/swagger-ui.html`
   - **REST API Base:** `http://localhost:8080/api/expenses`

4. **Stop the stack:**
   ```bash
   docker compose down
   ```
   *(Run `docker compose down -v` to also remove the persistent database volume.)*

### Alternative: One-click start scripts

Instead of typing Docker commands, you can use the provided scripts:

- **Windows:** double-click `start.bat`
- **Linux:** run `./start.sh` in a terminal (or right-click → “Run in Terminal”)

The script starts the stack, waits until the app is ready, opens the browser, and stops everything automatically when you press any key (Windows) or Enter (Linux) in the terminal.

## Running Tests

To run unit and controller tests locally (requires JDK 21+):

```bash
mvn test
```
