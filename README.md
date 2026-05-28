# Aurora Hotel Reservation System

Aurora Hotel is a Spring Boot web application for hotel guests and hotel administrators. Guests can browse rooms, create reservation requests, view their reservations, and ask questions through a chat widget. Administrators can manage rooms, reservations, chat replies, reusable conversation examples, and JSONL exports.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring MVC and Thymeleaf
- Spring Security
- Spring Data JPA
- PostgreSQL
- Bootstrap 5
- Lombok
- Maven

## Local Setup

Start PostgreSQL with Docker:

```powershell
docker start hotel-postgres
```

If the container does not exist yet:

```powershell
docker run --name hotel-postgres -e POSTGRES_DB=hotel_db -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -p 5432:5432 -d postgres:16
```

Run the application:

```powershell
.\mvnw spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Database Configuration

The local PostgreSQL configuration is stored in:

```text
src/main/resources/application.properties
```

Default local values:

- Database: `hotel_db`
- Username: `admin`
- Password: `admin`
- Port: `5432`

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so tables are created or updated from the JPA entities when the app starts.

## Default Accounts

The application creates starter accounts when the database is empty.

Admin accounts:

- `admin` / `admin123`
- `manager` / `admin123`

Guest account:

- `guest1` / `guest123`

## Main Pages

Guest pages:

- `/` - landing page
- `/rooms` - room listing with filters and pagination
- `/rooms/{id}` - room details
- `/reservations/new?roomId={id}` - reservation form
- `/reservations/my` - logged-in guest reservations

Admin pages:

- `/admin/dashboard`
- `/admin/rooms`
- `/admin/reservations`
- `/admin/chat`
- `/admin/conversations`
- `/admin/export`

## Features

- Role-based login for guests and admins
- Room browsing with filters, ratings, availability, and pagination
- Reservation creation with date validation and overlap prevention
- Admin reservation approval, cancellation, and staff notes
- Rule-based guest chat with admin fallback inbox
- Conversation builder with alternating user/assistant turns
- JSONL export for approved conversations
- Shared responsive Bootstrap layout

## Tests

Run:

```powershell
.\mvnw test
```
