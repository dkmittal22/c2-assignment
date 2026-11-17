# Testing Guidelines

## General

Every feature must include tests.

Tests should verify behavior rather than implementation details.

## Unit Tests

Use JUnit 5.

Use Mockito only when isolation from dependencies is useful.

Do not mock simple value objects or domain logic unnecessarily.

## Integration Tests

Use Spring Boot integration tests for:

- REST API behavior
- database persistence
- state-machine behavior
- validation
- repository integration

## State Machine

At minimum test:

OPEN -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
RESOLVED -> CLOSED
OPEN -> CANCELLED
IN_PROGRESS -> CANCELLED

And invalid:

CLOSED -> OPEN
RESOLVED -> OPEN
CANCELLED -> OPEN
OPEN -> RESOLVED
OPEN -> CLOSED
IN_PROGRESS -> CLOSED

## Validation

Test:

- blank title
- blank description
- invalid priority
- missing required fields

## API

Tests must verify:

- status code
- response body
- validation errors
- business errors

## Regression

When an AI-generated bug is discovered:

1. reproduce it with a test
2. fix implementation
3. keep the regression test
