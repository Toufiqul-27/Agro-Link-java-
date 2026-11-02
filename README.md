# AgroLink BD

A JavaFX desktop app for local agricultural crop listings, buyer orders, and shared truck capacity bookings.

**Status:** Student project / local development. Not production ready.

## Features

- Registration and role-based login (Farmer, Buyer, Arotdar, Admin)
- Farmer crop listings, buyer orders and order management
- Truck routes, cost calculations and capacity-safe bookings
- Admin dashboard
- Salted PBKDF2 password hashes for new users

## Stack

Java 21 LTS, JavaFX 21, Maven, MySQL 8.0.16+ and JDBC.

## Run locally (Windows PowerShell)

1. Install JDK 21 and MySQL 8.0+, and check Java with: java -version
2. Create the local schema using MySQL Workbench to execute **database/schema.sql**, or from a terminal:

~~~bash
mysql -u root -p < database/schema.sql
~~~

3. Create a local MySQL user with appropriate access to agrolink_db. Set these environment variables in the SAME PowerShell terminal used to run the application:

~~~powershell
$env:AGROLINK_DB_USER = "your_local_mysql_user"
$env:AGROLINK_DB_PASSWORD = "your_local_mysql_password"
$env:AGROLINK_DB_URL = "jdbc:mysql://localhost:3306/agrolink_db"
~~~

4. From the repository root:

~~~powershell
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
~~~

On Linux or macOS use export for these environment variables and ./mvnw for Maven.

The schema creates tables when missing; it does NOT migrate an existing database. Back up existing data before making schema changes.

## First-use flow

Register a Farmer, Buyer or Arotdar account, then log in with the chosen role. Farmers can list crops, and buyers can order available products.

Admin accounts cannot be created via public registration. If you need the admin dashboard locally, provision an authorized Admin account directly in the development database using a securely generated PBKDF2 password hash. There are deliberately no default administrator credentials. The schema also contains no demo users or trucks.

## Data tables

- users: account details, role, status, password hash
- products: crop listings and remaining stock
- orders: buyer orders and status
- trucks: routes, departure dates and shared capacity
- truck_bookings: truck reservations

All defined in [database/schema.sql](database/schema.sql).

## Security notes

**Important:** A database credential was committed in an earlier revision. Rotate that MySQL password immediately and review exposure. Replacing it in the latest files does not erase the secret from Git history.

Legacy rows may still contain plaintext passwords. Login temporarily verifies these and migrates them to PBKDF2 after a successful sign-in. For real users, require password resets and clean up legacy stored data. Never upload real credentials, NID datasets or database dumps.

This code is for educational use; production security, privacy, deployment and end-to-end testing still need work.

## Build verification

The JUnit tests can be run with the Maven wrapper. To fully verify the UI, configure a local MySQL instance and manually test registration/login, crop listing, ordering and truck bookings. No real MySQL or JavaFX UI test run is claimed here.

## Project structure

~~~text
src/main/java/com/agrolinkbd/agrolinkbd/          Application, controllers, models
src/main/resources/com/agrolinkbd/agrolinkbd/     FXML views
src/main/resources/com/agrolinkbd/css/            Styles
src/test/java/com/agrolinkbd/agrolinkbd/          Unit tests
database/schema.sql                               MySQL schema
~~~

## Limitations

No live GPS, payment gateway, cloud deployment or formal security audit.

**Author:** [Toufiqul Islam](https://github.com/Toufiqul-27)
