# Job Portal

A comprehensive job portal application built with Spring Boot that allows users to browse jobs, apply for positions, and manage their applications. Features include OAuth2 authentication, role-based access control, and a RESTful API.

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Database (PostgreSQL/MySQL/H2)

## Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/faseehsandhuu/jobportal.git
   ```

2. Navigate to the project directory:

   ```bash
   cd jobportal
   ```

3. Install dependencies:

   ```bash
   mvn clean install
   ```

4. Configure database settings in `src/main/resources/application.yaml`

## Usage

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Main Features

- **User Registration & Authentication**: Supports OAuth2 (Google) and traditional email/password registration
- **Role-based Access**: Distinct access levels for `APPLICANT` and `EMPLOYER` roles
- **Job Management**: Create, read, update, and delete job listings (restricted to `EMPLOYER` role)
- **Application Management**: Apply for jobs and view application status (for `APPLICANT` and `EMPLOYER` roles)
- **RESTful API**: Comprehensive API for job and application management

## Building

To build the project:

```bash
mvn clean compile
```

To create a JAR file:

```bash
mvn clean package
```

The built JAR will be available in the `target/` directory.

## Testing

Run all tests:

```bash
mvn test
```

Run tests with coverage:

```bash
mvn test jacoco:report
```

Test classes include:

- `ApplicationControllerTest`
- `JobControllerTest`
- `JwtTokenTest`
- `UserControllerTest`

## Project Structure

```
src/
├── main/
│   ├── java/com/redmath/jobportal/
│   │   ├── JobPortalApplication.java
│   │   ├── application/
│   │   │   ├── controller/ApplicationController.java
│   │   │   ├── dto/ApplicationDto.java
│   │   │   ├── model/Application.java
│   │   │   ├── repository/ApplicationRepository.java
│   │   │   └── service/ApplicationService.java
│   │   ├── auth/
│   │   │   ├── controller/UserController.java
│   │   │   ├── dtos/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── services/
│   │   ├── config/
│   │   ├── exceptions/
│   │   └── job/
│   │       ├── controller/JobController.java
│   │       ├── model/Job.java
│   │       ├── repository/JobRepository.java
│   │       └── service/
│   └── resources/
│       ├── application.yaml
│       ├── db/changelog/
│       └── templates/
└── test/java/com/redmath/jobportal/
```

## API Endpoints

### Authentication

- `GET /auth/register` - Displays the registration form
- `POST /auth/register` - Registers a new user (form-based)
- `POST /auth/api/register` - Registers a new user via API (returns JSON response)

### Jobs

- `GET /api/v1/jobs` - Retrieves all job listings
- `POST /api/v1/jobs/{id}` - Retrieves a specific job by ID
- `POST /api/v1/jobs` - Creates a new job (requires `EMPLOYER` role)
- `PUT /api/v1/jobs/{id}` - Updates a job (requires `EMPLOYER` role)
- `DELETE /api/v1/jobs/{id}` - Deletes a job (requires `EMPLOYER` role)

### Applications

- `POST /api/v1/application/{jobId}` - Submits an application for a job (requires `APPLICANT` role)
- `GET /api/v1/application/my` - Retrieves applications for the authenticated applicant (requires `APPLICANT` role)
- `GET /api/v1/application/recruiter/all` - Retrieves all applications for jobs posted by the authenticated employer (requires `EMPLOYER` role)
- `GET /api/v1/application/recruiter/job/{jobId}` - Retrieves applications for a specific job posted by the authenticated employer (requires `EMPLOYER` role)

## Configuration

Key configuration files:

- `application.yaml` - Main application configuration
- Database migrations in `src/main/resources/db/changelog/`
- Security configurations in `config/` package

### OAuth2 Setup

Configure Google OAuth2 in `application.yaml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
```

## Database

This project uses Liquibase for database migration management with separate changelogs for:

- User management (DDL & DML)
- Job management (DDL & DML)
- Application management (DDL & DML)

Run migrations:

```bash
mvn liquibase:update
```

## Authentication

The application supports multiple authentication methods:

- **OAuth2 Google Login**: Social login integration
- **JWT Token-based Authentication**: For secure API access (uses `bearerAuth`)
- **Role-based Authorization**: `APPLICANT` and `EMPLOYER` roles with specific endpoint access

## Dependencies

Key dependencies used in this project:

- Spring Boot - Application framework
- Spring Security - Authentication and authorization
- Spring Data JPA - Data persistence
- Liquibase - Database migration
- OAuth2 - Social login integration
- JWT - Token-based authentication

See `pom.xml` for the complete list of dependencies.

## Contributing

Contributions are welcome! Please follow the standard fork-and-pull request workflow.

## License

This project is licensed under the MIT License.