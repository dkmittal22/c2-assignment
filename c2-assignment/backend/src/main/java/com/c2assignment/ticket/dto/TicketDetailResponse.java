package com.c2assignment.ticket.dto;

import com.c2assignment.comment.dto.CommentResponse;
import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ticket detail representation used by GET /api/v1/tickets/{id}. Includes the
 * ordered comments array (spec/api-contract.md §5.3).
 */
public record TicketDetailResponse(
        UUID id,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
}