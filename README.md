# AgroLink BD

A Bangladesh-focused agriculture marketplace and shared transport project.

## New: Deployable web application

A modern **Next.js 16 + Supabase PostgreSQL** web MVP is available in **[web/](web/)**.

- Mobile-friendly public crop marketplace and role-based dashboards
- Farmer listings, buyer orders and private order discussions
- Shared transport route discovery and capacity booking
- Supabase Auth, row-level security and transaction-safe SQL operations
- GitHub Actions web build and Vercel deployment instructions

**[Read the complete web setup and deployment guide →](web/README.md)**

The web app is a student pilot and is **not yet hosted or tested with a production database**. Deploying requires your own Supabase project and Vercel configuration. No actual payments or deliveries are processed.

## Legacy JavaFX desktop application

The original desktop version (JavaFX, Maven and MySQL) is preserved at repository root for learning and compatibility.

### Local requirements

- Java 21, MySQL 8.0.16+
- Run [database/schema.sql](database/schema.sql) in a **local** MySQL instance (do not overwrite an existing database)
- Set AGROLINK_DB_USER, AGROLINK_DB_PASSWORD, and optionally AGROLINK_DB_URL

Windows PowerShell:

~~~powershell
$env:AGROLINK_DB_USER = "your_local_mysql_user"
$env:AGROLINK_DB_PASSWORD = "your_local_mysql_password"
$env:AGROLINK_DB_URL = "jdbc:mysql://localhost:3306/agrolink_db"
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
~~~

**Security notice:** A MySQL credential was hardcoded in earlier Git history. Rotate it and never reuse it. Passwords of legacy demo users might require migration/reset.

## Repository layout

~~~text
web/                  Next.js + Supabase web MVP (deploy this)
database/             Legacy JavaFX MySQL schema
src/main/             Legacy JavaFX application
src/test/             Java unit tests
.github/workflows/    Java and web CI
~~~

**Author:** [Toufiqul Islam](https://github.com/Toufiqul-27)
