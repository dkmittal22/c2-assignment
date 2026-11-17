# API Standards

Base path:

/api/v1

Use nouns rather than verbs.

Examples:

GET    /api/v1/tickets
POST   /api/v1/tickets
GET    /api/v1/tickets/{id}
PATCH  /api/v1/tickets/{id}
POST   /api/v1/tickets/{id}/comments
POST   /api/v1/tickets/{id}/transitions

Filtering:

GET /api/v1/tickets?status=OPEN

Searching:

GET /api/v1/tickets?search=database

Responses must use DTOs.

Do not expose persistence entities.

Use appropriate HTTP status codes.

201 Created
200 OK
204 No Content
400 Bad Request
404 Not Found
409 Conflict
500 Internal Server Error

Business-rule conflicts should use 409 Conflict.

Validation failures should use 400 Bad Request.

API error structure:

{
  "timestamp": "...",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/tickets",
  "fieldErrors": {
    "title": "Title is required"
  }
}
