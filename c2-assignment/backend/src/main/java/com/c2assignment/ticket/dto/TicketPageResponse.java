package com.c2assignment.ticket.dto;

import java.util.List;

/**
 * Paginated list response for GET /api/v1/tickets (spec/api-contract.md §4).
 */
public record TicketPageResponse(
        List<TicketResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}