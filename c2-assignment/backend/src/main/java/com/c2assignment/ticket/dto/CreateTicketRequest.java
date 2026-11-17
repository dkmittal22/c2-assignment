package com.c2assignment.ticket.dto;

import com.c2assignment.ticket.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create ticket request (spec/api-contract.md §5.1).
 * Allowed fields only: title, description, priority, assignee.
 * The field set is fixed — `status`, `id`, `createdAt`, `updatedAt` and any
 * unknown field are rejected by Jackson as unknown properties (BAD_REQUEST).
 */
public record CreateTicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        Priority priority,

        @Size(max = 100, message = "Assignee must not exceed 100 characters")
        String assignee
) {
}