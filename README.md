![Language](https://img.shields.io/badge/language-Java%20-blue.svg)
![Technologies](https://img.shields.io/badge/technologies-Spring_boot%20-green.svg)
![Technologies](https://img.shields.io/badge/technologies-Spring_MVC%20-green.svg)
![Technologies](https://img.shields.io/badge/technologies-Spring_Security%20-green.svg)
![Technologies](https://img.shields.io/badge/technologies-Spring_Data_jpa%20-green.svg)
![Technologies](https://img.shields.io/badge/technologies-Thymeleaf_&_Bootstrap%20-purple.svg)

# Expenses-Tracker-WebApp
## Overview
The Expenses Tracker App is a robust financial management solution developed using cutting-edge technologies such as Spring Boot, Spring Security, and MySQL. With user authentication and authorization features, users can securely sign up, sign in, and perform CRUD operations on their expenses. The app's intuitive interface, powered by Thymeleaf and Bootstrap, ensures a seamless user experience. The filtering functionality allows users to efficiently organize and analyze their financial data. Explore the power of streamlined expense tracking and financial control with this feature-rich application.<br> (Screenshots below for more illustration)

## Technologies Used
- Java
- Spring boot
- Spring MVC
- Spring Security
- Spring Data (JPA)
- MySQL
- Thymeleaf
- Bootstrap

## Features
- **User Authentication and Authorization:** Securely sign up, sign in, and access the app with built-in authentication and authorization.
- **CRUD Operations:** Perform essential financial tracking actions such as adding, reading, updating, and deleting expenses.
- **Filtering:** Utilize the filtering feature to efficiently sort and view expenses based on various criteria.

## Getting Started
**Prerequisite:** Install Java 21 (LTS) and verify it is available in your terminal:
```powershell
java -version
```

1. **Clone the Repository:**
`git clone https://github.com/your-username/expenses-tracker.git`

2. **Configure Local MySQL:**
Start MySQL through XAMPP or your local MySQL service, then create the database if it does not already exist:
```sql
CREATE DATABASE expenses_tracker;
```
The local default connection is `localhost:3306/expenses_tracker` as `root`. If your MySQL account uses a password, set it in PowerShell before starting the app (replace the example with your actual local password):
```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
```
If you use a different MySQL account, set its username as well:
```powershell
$env:DB_USERNAME = "your-mysql-username"
```
For a local MySQL account with no password, set `$env:DB_PASSWORD = ""`. Credentials are read from environment variables and are not stored in `application.properties`.

3. **Build and Run:**
From the project root, start the application with the Maven Wrapper (no separate Maven installation required):
```powershell
.\mvnw.cmd spring-boot:run
```
Hibernate updates the tables on startup. The database itself must exist before the application connects.

4. **Access the App:**
Open your web browser and navigate to `http://localhost:8081`.

## Deploy on Render with Hosted MySQL

Render runs the application from the repository's `Dockerfile`. Render's managed relational database is PostgreSQL, so this MySQL application needs a separately hosted MySQL database. XAMPP on your computer is local and is not an internet-accessible production database.

1. Push the project, including `Dockerfile` and `render.yaml`, to GitHub.
2. Create a hosted MySQL database with a provider that permits connections from Render. Keep its hostname, port, database name, username, and password private.
3. In Render, choose **New > Blueprint**, connect the GitHub repository, and deploy the `expenses-tracker` service defined in `render.yaml`.
4. In the Render service's **Environment** settings, set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Use the connection details supplied by the database provider:
   ```text
   DB_URL=jdbc:mysql://<mysql-host>:<mysql-port>/<database-name>?sslMode=REQUIRED
   DB_USERNAME=<mysql-username>
   DB_PASSWORD=<mysql-password>
   ```
   Use any provider-specific TLS or connection parameters required by your database provider. Never commit production credentials.
5. Render supplies the application port at runtime; the Docker entry point uses it automatically. After deployment, open the public Render URL and test registration, login, and expense operations.

The existing `sql_script.sql` contains MySQL table definitions. Back up any existing database before applying schema changes. A new hosted database also needs the application's initial role and category records before registration and expense entry can work.

## ScreenShots
![Example Image](screenshots/1.png) <br>
![Example Image](screenshots/2-2.png) <br>
![Example Image](screenshots/3-3.png) <br>
![Example Image](screenshots/4-4.png) <br>
![Example Image](screenshots/5-5.png) <br>
![Example Image](screenshots/6-6.png) <br>
![Example Image](screenshots/7.png) <br>
![Example Image](screenshots/8.png) <br>

## Contributions
Contributions are welcome! If you find a bug or have suggestions for improvement, feel free to open an issue or create a pull request.

## License
This project is licensed under the MIT License.
