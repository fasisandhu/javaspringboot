# 📰 News App - Spring Boot + JWT

A backend RESTful API for a News Platform built with **Spring Boot 3**, **Java 21**, and **JWT authentication**.  
Supports role-based access control for Admins, Editors, and Users.

---

## 🚀 Features

- ✅ JWT Authentication (Login, Register)
- ✅ Role-based access: `ADMIN`, `EDITOR`, `USER`
- ✅ Create, delete, and view news articles
- ✅ Category management (Admin only)
- ✅ Public API for browsing/filtering news
- ✅ Swagger UI documentation (with JWT support)

---

## 📦 Tech Stack

- **Java 21**
- **Spring Boot 3.x**
- **Spring Security + JWT**
- **Spring Data JPA**
- **H2 In-Memory Database** (development)
- **Maven** for dependency management
- **Swagger (Springdoc OpenAPI)** for API documentation

---

## 📁 Project Structure (Main Modules)

```bash
src/main/java/com/faseeh/newsapp/
├── auth          # Register / Login
├── config        # Security & Swagger config
├── user          # User + roles
├── category      # Category management
├── news          # News creation, listing, filtering
```
# 🔐 Authentication

## Roles

| Role   | Permissions                          |
|--------|--------------------------------------|
| ADMIN  | Manage categories, users, full access |
| EDITOR | Post/edit their own news             |
| USER   | Read-only access                     |

## Endpoints

| Method | Endpoint         | Description            |
|--------|------------------|------------------------|
| POST   | `/auth/register` | User registration      |
| POST   | `/auth/login`    | Login and get JWT token |

---

# 📂 News API

## Public

| Method | Endpoint                          | Description           |
|--------|-----------------------------------|-----------------------|
| GET    | `/news`                           | List all news         |
| GET    | `/news/category/{id}`             | Filter by category    |
| GET    | `/news/editor/{id}`               | Filter by editor      |
| GET    | `/news/search?keyword=xyz`        | Search news           |

## Protected (JWT Required)

| Method | Endpoint         | Role   | Description           |
|--------|------------------|--------|-----------------------|
| POST   | `/news`          | EDITOR | Editor creates news   |
| GET    | `/news/mine`     | EDITOR | Editor's own news     |
| PUT    | `/news/{id}`     | EDITOR | Editor updates news   |

---

# 📁 Category API

| Method | Endpoint             | Role  | Description         |
|--------|----------------------|-------|---------------------|
| GET    | `/categories`        | Public | List all categories |
| POST   | `/categories`        | ADMIN | Create a category   |
| DELETE | `/categories/{id}`   | ADMIN | Delete a category   |

---

# 📘 API Docs – Swagger

Visit: http://localhost:8080/swagger-ui.html

Use the 🔐 **Authorize** button and paste your Bearer token:  
`Bearer eyJhbGciOiJIUzI1...`


