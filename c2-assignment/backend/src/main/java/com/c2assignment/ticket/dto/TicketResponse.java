package com.c2assignment.ticket.dto;

import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.TicketStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Ticket representation WITHOUT comments. Used by create, PATCH, transition and
 * list responses (spec/api-contract.md §4 "TicketResponse without comments").
 */
public record TicketResponse(
        UUID id,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {
}