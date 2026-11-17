package com.c2assignment.ticket.dto;

import com.c2assignment.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Transition request body (spec/api-contract.md §5.6). Allowed field only: toStatus.
 */
public record TransitionRequest(
        @NotNull(message = "toStatus is required")
        TicketStatus toStatus
) {
}