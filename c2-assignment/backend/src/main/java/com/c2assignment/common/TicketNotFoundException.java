package com.c2assignment.common;

/**
 * Raised when a valid UUID has no matching ticket. Maps to HTTP 404 NOT_FOUND.
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(java.util.UUID id) {
        super("Ticket not found: " + id);
    }
}