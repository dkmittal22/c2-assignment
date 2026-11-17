package com.c2assignment.ticket;

/**
 * Ticket lifecycle states (spec/state-machine.md §2, spec/data-model.md §3).
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}