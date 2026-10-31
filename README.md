<div align="center">
  <img src="https://raw.githubusercontent.com/Toufiqul-27/portfolio-1/main/assets/github-banner.svg" alt="Toufiqul Islam developer banner" width="100%"/>
</div>

---

# AgroLink BD

**AgroLink BD** is a JavaFX desktop application exploring how farmers, buyers and administrators can manage agricultural products, orders and shared truck bookings in one place.

> **Status:** Student project / local development. This repository is not a production-ready service.

## Features in the codebase

- **Account flows:** registration, login and role-based screens.
- **Farmer dashboard:** view, add and manage crop listings.
- **Buyer dashboard:** browse produce, filter listings and place orders.
- **Order management:** view orders and update statuses.
- **Truck booking:** browse routes and capacity, estimate shared transport costs and manage bookings.
- **Admin dashboard:** view users, orders and trucks.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java |
| Desktop UI | JavaFX + FXML |
| Database | MySQL (JDBC) |
| Build tool | Maven |
| Tests | JUnit dependencies configured in `pom.xml` |

## Project structure

```text
src/main/
├── java/com/agrolinkbd/agrolinkbd/      # App, models, controllers, database access
└── resources/com/agrolinkbd/agrolinkbd/ # FXML views and styles
```

## Running locally

1. Install **JDK 25**, **Maven** (or use the included Maven wrapper) and a local **MySQL** server. The project currently targets Java 25 in `pom.xml`.
2. Create a local MySQL database named `agrolink_db` and configure the database tables used by the application. **A database schema/migration script is not yet included in this repository.**
3. Set environment variables for the MySQL connection (do not commit the values):

   **PowerShell:**

   ```powershell
   $env:AGROLINK_DB_USER = "your_mysql_user"
   $env:AGROLINK_DB_PASSWORD = "your_local_mysql_password"
   $env:AGROLINK_DB_URL = "jdbc:mysql://localhost:3306/agrolink_db"
   ```

   **macOS / Linux:**

   ```bash
   export AGROLINK_DB_USER="your_mysql_user"
   export AGROLINK_DB_PASSWORD="your_local_mysql_password"
   export AGROLINK_DB_URL="jdbc:mysql://localhost:3306/agrolink_db"
   ```

4. From the repository root, run:

```bash
./mvnw clean javafx:run
```

On Windows PowerShell, use `./mvnw.cmd clean javafx:run`.

**Security note:** Database credentials are now read from `AGROLINK_DB_USER`, `AGROLINK_DB_PASSWORD` and optional `AGROLINK_DB_URL`. A password was previously committed: **rotate it now**. Old commits may still expose it until repository history is carefully cleaned. Never commit real credentials.

## What I want to improve

- Add a reproducible database setup script.
- Review and clean credential exposure in earlier Git history after rotating the password.
- Improve form validation, automated tests and error handling.
- Add screenshots and a short demo of the application workflow.

## Author

**Toufiqul Islam** — [GitHub profile](https://github.com/Toufiqul-27)
