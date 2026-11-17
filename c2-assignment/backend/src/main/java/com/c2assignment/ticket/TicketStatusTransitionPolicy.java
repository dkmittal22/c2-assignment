package com.c2assignment.ticket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure domain policy encoding the full transition matrix from spec/state-machine.md §4-6.
 *
 * <p>This class has no HTTP or persistence dependencies and is fully unit-testable.</p>
 */
public final class TicketStatusTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        ALLOWED.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        ALLOWED.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED));
        // CLOSED and CANCELLED are terminal: no outbound transitions (spec §5).
        ALLOWED.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        ALLOWED.put(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));
    }

    private TicketStatusTransitionPolicy() {
    }

    /**
     * @param from current status
     * @param to   requested target status
     * @return true if and only if the transition is allowed by the state machine
     */
    public static boolean isAllowed(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.get(from).contains(to);
    }
}