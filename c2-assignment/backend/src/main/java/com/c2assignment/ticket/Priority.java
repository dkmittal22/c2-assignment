package com.c2assignment.ticket;

/**
 * Ticket priority (spec/data-model.md §3). Default when omitted on create is MEDIUM.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}