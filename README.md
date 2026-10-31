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
3. Review the connection configuration in `Database.java` for your local environment.
4. From the repository root, run:

```bash
./mvnw clean javafx:run
```

On Windows PowerShell, use `./mvnw.cmd clean javafx:run`.

**Security note:** Database credentials should not be committed to source control. Migrate connection settings to environment variables and rotate any credentials already committed before deploying or sharing access. Never commit real production credentials.

## What I want to improve

- Add a reproducible database setup script.
- Externalize database credentials and add safer configuration handling.
- Improve form validation, automated tests and error handling.
- Add screenshots and a short demo of the application workflow.

## Author

**Toufiqul Islam** — [GitHub profile](https://github.com/Toufiqul-27)
