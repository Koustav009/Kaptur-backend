# 🦉 Fotoowl Clone Backend

Welcome to the **Fotoowl Clone Backend**! This project is a robust, production-ready backend for a photo management application. It's built with modern Java technologies and follows industry-standard architectural patterns.

This guide is designed for **newbie and junior developers** to help you understand what's under the hood and how to get everything running on your local machine.

---

## 🚀 Tech Stack (What we use & why)

*   **Java 21:** The latest LTS (Long Term Support) version of Java, offering great performance and modern features like Virtual Threads.
*   **Spring Boot 4.0.x:** The "magic" framework that handles all the heavy lifting (server setup, dependency management, etc.) so we can focus on writing business logic.
*   **Spring Data JPA / Hibernate:** An ORM (Object Relational Mapper). It allows us to talk to the MySQL database using Java objects instead of writing raw SQL.
*   **MySQL:** Our relational database where we store user data and photo information.
*   **Spring Security & JWT:** Handles login and signup securely. **JWT (JSON Web Token)** is used to keep users logged in without storing session data on the server.
*   **OAuth2 (Google Login):** Allows users to sign in using their Google accounts.
*   **Lombok:** A library that uses annotations (like `@Data`, `@Getter`) to automatically generate boilerplate code like getters, setters, and constructors.
*   **SpringDoc (Swagger UI):** Automatically generates documentation for our API so you can test it directly from your browser.

---

## 🛠️ Prerequisites

Before you start, make sure you have the following installed:
1.  **JDK 21** (e.g., from [Adoptium](https://adoptium.net/))
2.  **MySQL Server 8.0+**
3.  **An IDE** (IntelliJ IDEA is highly recommended for Java development, but VS Code works too!)
4.  **Maven** (You don't *need* to install this separately as we use the `mvnw` wrapper included in the project).

---

## ⚙️ Setup & Installation

### 1. Database Setup
1.  Open your MySQL terminal or a GUI like MySQL Workbench.
2.  Create a new database named `fotoowl`:
    ```sql
    CREATE DATABASE fotoowl;
    ```

### 2. Configuration
Open `src/main/resources/application.properties` and update the database credentials if they differ from yours:
```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

> **Note:** For Google Login to work, you'll need to create a project in the [Google Cloud Console](https://console.cloud.google.com/) and replace `YOUR_GOOGLE_CLIENT_ID` and `YOUR_GOOGLE_CLIENT_SECRET`.

### 3. Build the Project
Open your terminal in the project root and run:
```powershell
.\mvnw.cmd clean install
```
*(On Mac/Linux use `./mvnw clean install`)*

---

## 🏃 Running the Application

To start the server, run:
```powershell
.\mvnw.cmd spring-boot:run
```
The server will start on **`http://localhost:8080`**.

---

## 📂 Project Structure (A Junior's Guide)

We follow a **Layered Architecture**. Think of it like a restaurant:

*   **`controllers/` (The Waiter):** Receives requests from the user (like "I want to login") and sends back a response.
*   **`services/` (The Chef):** Does the actual work. It handles the logic, checks if passwords match, etc.
*   **`repository/` (The Pantry):** The only layer that talks to the database. It fetches or saves data.
*   **`model/` (The Ingredients):** Defines how our data looks (e.g., a `User` has an `email`, `password`, and `id`).
*   **`dto/` (Data Transfer Objects):** Simple objects used to send data back and forth between the frontend and backend (e.g., `LoginRequest`).
*   **`security/` (The Bouncer):** Protects our API. It checks if the user's JWT token is valid before letting them through.

---

## 🛣️ API Endpoints

Once the app is running, you can explore the full API documentation at:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### Main Authentication Endpoints:
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/auth/register` | Create a new user account |
| `POST` | `/auth/login` | Login and receive a JWT token |
| `GET` | `/auth/test` | Check if the auth server is alive |

---

## 💡 Learning Tips for Juniors
1.  **Check the logs:** If something breaks, look at the terminal where you ran the app. Spring Boot gives very detailed error messages.
2.  **Lombok is your friend:** If you see a class with no getters or setters but they are being used elsewhere, look for `@Data` at the top of the class.
3.  **H2 Console (Optional):** If you ever want to use an in-memory database for testing, Spring Boot makes it very easy to swap MySQL for H2.

---

Happy Coding! 🦉✨
