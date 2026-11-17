# Java Spring Boot Guidelines

## Java

- Use Java 21.
- Prefer immutable data structures where practical.
- Use records for immutable DTOs where appropriate.
- Use enums for finite domain states.
- Avoid unnecessary Optional usage in fields or method parameters.
- Use meaningful names.
- Prefer small methods with one responsibility.
- Do not suppress compiler warnings without justification.

## Spring Boot

- Follow standard Spring layered architecture.
- Controllers handle HTTP concerns only.
- Services contain business rules.
- Repositories handle persistence.
- DTOs must be used at API boundaries.
- Do not expose JPA entities directly from REST controllers.
- Business rules must not be implemented inside controllers.
- Use constructor injection.
- Avoid field injection.

## Error Handling

- Use centralized exception handling.
- API errors must have a consistent structure.
- Validation errors must identify invalid fields.
- Business-rule violations must return meaningful HTTP responses.

## Persistence

- Use JPA repositories.
- Database schema changes must be version controlled.
- Do not rely on manually modifying the database.

## State Machine

Ticket status transitions are domain rules.

Valid transitions:

OPEN -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
RESOLVED -> CLOSED

OPEN -> CANCELLED
IN_PROGRESS -> CANCELLED

Invalid transitions must be rejected.

The transition rule must be implemented in the backend domain/service layer and must never depend on frontend validation.

## Architecture

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

Controllers must not directly access repositories when business logic is required.

## Testing

Every business rule must have automated tests.

State transitions require integration tests.