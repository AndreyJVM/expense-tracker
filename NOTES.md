# Developer Notes

## Project Overview
This expense tracker was built primarily with Java. It uses Spring Boot and JPA for business logic and data persistence, exposing REST endpoints that feed a JavaScript/HTML front-end interface.

## Month 1 Recap
* **Initial Concept:** This project originally started as just a simple CLI application. My purpose was to learn and demonstrate back-end logic, as well as some programming concepts like loose coupling, separation of responsibilities, etc.
* **Overhaul:** After adding extra functionality and migrating a lot of the verbose logic to Spring Boot, I started seeing this application as more than just an exercise in back-end programming. That's when I decided to simplify the user experience by containerizing the whole thing in Docker and even adding a Web UI.
* **Current State:** The app now handles CRUD operations through a visual interface. All the information is available at a glance, including an expense table and a category chart (using MariaDB, Chart.js, and Tailwind CSS). It features optional user accounts with JWT, custom category, monthly budgets with progress tracking, pagination, recurring expenses, dark mode, EN/PT-BR localization, and USD/BRL currency switching. Data flows from the front-end UI, through REST endpoints, down to the repository layer. 
* **AI Usage Note:** Built with AI assistance for code implementation and boilerplate generation. But the architecture, data modeling, feature scope and debugging are directed manually.

---

## Dev Log

### [November 1–7 2026] - Recurring Expenses
* **What changed:** Added full support for recurring expenses (weekly / bi-weekly / monthly). New `RecurringExpense` entity, automatic generation via `@Scheduled` job + startup, optional "stop series" on delete, and UI controls (checkbox + frequency selector) on both Add and Edit forms. Also includes a small recurring icon in the transaction table.
* **Why:** Recurring transactions are a core feature of any real expense tracker. This also demonstrates scheduled tasks, more complex entity relationships, and keeping the front-end and back-end in sync when the template changes.

### [October 20–28, 2026] - Categories as Entity + Monthly Budgets + UX polish
* **What changed:** Categories moved from a fixed enum to a real `Category` table (system categories + user-created ones). Added monthly budget limits per category with progress bars and over-budget warnings. Implemented progressive "Show more" pagination, improved date/amount localization, completed Portuguese translations, and added one-click start scripts for Windows and Linux.
* **Why:** This update is huge. I polished the back-end, added some much needed pagination to the front-end, and improved the user experienced with several useful features.

### [October 16-19, 2026] - Pagination and Sorting (Backend)
* **What changed:** The REST Controller now accepts pagination and sorting parameters. The Service layer was also refactored appropriately. All parameters can be easily viewed and changed in Swagger.
* **Why:** This improvement makes the backend more robust. Instead of storing the expenses and presenting them in their raw form by order of insertion, this update introduces real sorting and custom pagination in the backend.

### [October 13, 2026] - Live Demo on Render
* **What changed:** Deployed the application to Render (free tier) using PostgreSQL. Added environment-variable based configuration so the same codebase works with both MariaDB (local) and Postgres (production).
* **Why:** To provide a public live demo for easy access and testing.

### [October 2–3, 2026] - Optional User Accounts & JWT Authentication
* **What changed:** Added Spring Security + JJWT. Users can now optionally register and log in. Expenses are fully isolated per account while still allowing anonymous (guest) usage. Introduced an ExpenseResponse DTO for cleaner API responses.
* **Why:** To demonstrate authentication, authorization, and multi-user data separation. This is good for shared machines, and it's also a staple of financial apps.

### [September 24, 2026] - UI Personalization & Dark Mode
* **What changed:** Added a dark mode toggle (with OS system color scheme auto-detection and `localStorage` persistence), a multi-currency switcher (USD/BRL), and full localization for English and Portuguese. Updated Chart.js to match the active theme.
* **Why:** Dark mode is important for accessibility, and currency/language switchers are crucial for localization, especially for banking/analytics apps.

### [September 14–16, 2026] - Web UI Implementation
* **What changed:** Added a visual interface that handles adding, deleting, viewing and filtering expenses.
* **Why:** The back-end functionality was complete when I finished the REST endpoints, but the program didn't feel like a real expense tracker without a visual UI. My main priority was making all crucial information accessible in a single modern dashboard.
