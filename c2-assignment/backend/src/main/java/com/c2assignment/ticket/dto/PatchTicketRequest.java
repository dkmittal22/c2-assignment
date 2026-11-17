package com.c2assignment.ticket.dto;

import com.c2assignment.ticket.Priority;
import jakarta.validation.constraints.Size;

/**
 * Patch ticket request (spec/api-contract.md §5.4). All fields optional; at least
 * one of title / description / priority / assignee must be present. `status`,
 * `id`, `createdAt`, `updatedAt` and unknown fields are rejected by Jackson
 * (BAD_REQUEST). Null fields are treated as "not changed"; present-but-blank
 * fields are rejected by the service layer (VALIDATION_ERROR). Note: bean
 * validation must NOT use @NotBlank here, because @NotBlank rejects null.
 */
public record PatchTicketRequest(
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        Priority priority,

        @Size(max = 100, message = "Assignee must not exceed 100 characters")
        String assignee
) {
}