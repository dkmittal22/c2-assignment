package com.c2assignment.ticket;

import com.c2assignment.comment.dto.CommentResponse;
import com.c2assignment.ticket.dto.TicketDetailResponse;
import com.c2assignment.ticket.dto.TicketResponse;

import java.util.List;

/**
 * Maps Ticket entities to API DTOs. Entities are never exposed directly.
 */
public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public static TicketDetailResponse toDetailResponse(Ticket ticket, List<CommentResponse> comments) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments
        );
    }
}