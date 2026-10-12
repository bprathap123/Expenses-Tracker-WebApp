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
   DB_URL=jdbc:mysql://mysql-21f11ff1-prathap7780-7d47.f.aivencloud.com:15170/defaultdb?sslMode=REQUIRED
   DB_USERNAME=<mysql-username>
   DB_PASSWORD=<mysql-password>
   ```
   Copy `DB_USERNAME` and `DB_PASSWORD` from Aiven into Render's private environment settings; never commit or share those production credentials. The application initializes the standard role and expense categories automatically when it starts.
5. Render supplies the application port at runtime; the Docker entry point uses it automatically. After deployment, open the public Render URL and test registration, login, and expense operations.

The existing `sql_script.sql` contains MySQL table definitions. Back up any existing database before applying schema changes. The application creates or updates its tables and initializes the standard role and categories on startup; existing user and expense data is not copied from your local database.

### Enable social sign-in and password reset

The Google and Facebook sign-in buttons are shown only after each provider has been configured. Register a web OAuth client with each provider and add these exact authorized redirect URLs:

```text
https://expenses-tracker-4lnu.onrender.com/login/oauth2/code/google
https://expenses-tracker-4lnu.onrender.com/login/oauth2/code/facebook
```

Add the resulting values in the Render service's **Environment** settings. Do not commit these values or send them in chat:

```text
GOOGLE_CLIENT_ID=<Google web client ID>
GOOGLE_CLIENT_SECRET=<Google web client secret>
FACEBOOK_CLIENT_ID=<Meta app ID>
FACEBOOK_CLIENT_SECRET=<Meta app secret>
```

For Facebook, enable the Facebook Login product and add the Facebook callback URL to its valid OAuth redirect URI list. The Facebook app must be live and permitted to serve your users.

For password reset, configure a Gmail account with two-step verification and create a Google **App Password** for this application. In Render set:

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your Gmail address>
MAIL_PASSWORD=<the 16-character Google App Password>
APP_PUBLIC_BASE_URL=https://expenses-tracker-4lnu.onrender.com
```

Use the App Password, not your normal Gmail password. Until these values are set, social buttons stay hidden, and password-reset emails cannot be sent. After changing Render environment settings, deploy/restart the service and test using an account with an email address you can access.

### View the hosted data in phpMyAdmin

Production records are in the Aiven MySQL database configured by `DB_URL`, not in the MySQL database local to XAMPP. The data remain in Aiven; phpMyAdmin is only an administration tool and must connect to that same Aiven server to show them.

To use your local XAMPP phpMyAdmin:

1. In Aiven, open the MySQL service's connection information and download its CA certificate.
2. Copy the certificate into a protected local folder, then configure an additional server in `C:\xampp\phpMyAdmin\config.inc.php` (back up this file first). Use the hostname, port, and database username shown in Aiven; do not use `localhost` or your XAMPP credentials:
   ```php
   $i++;
   $cfg['Servers'][$i]['auth_type'] = 'cookie';
   $cfg['Servers'][$i]['host'] = '<Aiven MySQL hostname>';
   $cfg['Servers'][$i]['port'] = '<Aiven MySQL port>';
   $cfg['Servers'][$i]['connect_type'] = 'tcp';
   $cfg['Servers'][$i]['ssl'] = true;
   $cfg['Servers'][$i]['ssl_ca'] = 'C:/path/to/your/aiven-ca.pem';
   ```
3. Restart Apache in XAMPP, open phpMyAdmin, choose the Aiven server, and sign in with the Aiven database username and password. Select `defaultdb` to see the production tables.

Keep the Aiven password private. Local XAMPP data and hosted Aiven data are separate databases; importing or copying local records is a separate operation and is not done automatically.

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
