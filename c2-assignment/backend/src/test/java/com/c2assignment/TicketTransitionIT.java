package com.c2assignment;

import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import com.c2assignment.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Transition API ITs covering the full state machine matrix over HTTP + DB
 * (spec/test-strategy.md §5.3; state-machine §4-9, SM-1…SM-15).
 */
@Transactional
class TicketTransitionIT extends AbstractIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @ParameterizedTest(name = "{0} -> {1} succeeds")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, CANCELLED",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, CANCELLED",
            "RESOLVED, CLOSED"
    })
    void validTransitionsSucceed(String from, String to) throws Exception {
        UUID id = seed(from);

        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\": \"" + to + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to))
                .andExpect(jsonPath("$.comments").doesNotExist());

        assertPersistedStatus(id, TicketStatus.valueOf(to));
    }

    @ParameterizedTest(name = "{0} -> {1} rejected with 409 and persisted state unchanged")
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, CLOSED",
            "IN_PROGRESS, OPEN",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, CANCELLED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CANCELLED",
            "CANCELLED, OPEN",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, RESOLVED",
            "CANCELLED, CLOSED"
    })
    void invalidTransitionsReturnConflictAndDoNotPersist(String from, String to) throws Exception {
        UUID id = seed(from);

        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\": \"" + to + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Status transition from " + from + " to " + to + " is not allowed"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        assertPersistedStatus(id, TicketStatus.valueOf(from));
    }

    @ParameterizedTest(name = "self-transition {0} -> {0} rejected")
    @CsvSource({"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"})
    void selfTransitionsReturnConflict(String status) throws Exception {
        UUID id = seed(status);

        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\": \"" + status + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));

        assertPersistedStatus(id, TicketStatus.valueOf(status));
    }

    @Test
    void transitionMissingTicketReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus": "IN_PROGRESS"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void transitionMalformedUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/not-a-uuid/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus": "IN_PROGRESS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void transitionInvalidEnumReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus": "PENDING"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.toStatus").exists());
    }

    @Test
    void transitionMissingToStatusReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.toStatus").exists());
    }

    @Test
    void transitionUnknownFieldReturnsBadRequest() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/transitions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toStatus": "IN_PROGRESS", "reason": "Because"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    private UUID seed(String status) {
        Ticket ticket = new Ticket(
                UUID.randomUUID(),
                "title",
                "description",
                Priority.MEDIUM,
                TicketStatus.valueOf(status),
                null,
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T08:00:00Z"));
        return ticketRepository.save(ticket).getId();
    }

    private void assertPersistedStatus(UUID id, TicketStatus expected) throws Exception {
        assertThat(ticketRepository.findById(id).orElseThrow().getStatus()).isEqualTo(expected);
    }
}