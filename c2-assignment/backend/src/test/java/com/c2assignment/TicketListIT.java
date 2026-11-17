package com.c2assignment;

import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import com.c2assignment.ticket.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * List endpoint ITs: pagination, search, status filter, sorting
 * (spec/test-strategy.md §5.1, api-contract §5.2, FR-2).
 */
@Transactional
class TicketListIT extends AbstractIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    private Instant time(String iso) {
        return Instant.parse(iso);
    }

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        ticketRepository.save(ticket("Database issue", "Logic in data layer", Priority.HIGH, TicketStatus.OPEN, "2026-09-01T08:00:00Z", "2026-09-01T08:00:00Z"));
        ticketRepository.save(ticket("Login broken", "Cannot log in on mobile", Priority.CRITICAL, TicketStatus.OPEN, "2026-09-02T08:00:00Z", "2026-09-02T08:00:00Z"));
        ticketRepository.save(ticket("Closed case", "Already resolved", Priority.LOW, TicketStatus.CLOSED, "2026-09-03T08:00:00Z", "2026-09-03T08:00:00Z"));
    }

    @Test
    void defaultsToPageZeroSizeTwenty() throws Exception {
        for (int i = 0; i < 20; i++) {
            ticketRepository.save(ticket("Seed " + i, "desc", Priority.MEDIUM, TicketStatus.OPEN,
                    "2026-09-04T00:00:00Z", "2026-09-04T00:00:00Z"));
        }
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void sizeGreaterThanMaxIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void sizeZeroIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void negativePageIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void nonIntegerPageIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?page=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void statusFilterReturnsOnlyMatching() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[1].status").value("OPEN"));
    }

    @Test
    void invalidStatusQueryIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?status=BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void emptySearchIsTreatedAsNoFilter() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?search="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void searchIsCaseInsensitiveSubstringOnTitle() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?search=DaTa")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Database issue"));
    }

    @Test
    void searchMatchesDescription() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?search=mobile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Login broken"));
    }

    @Test
    void searchAndStatusCombineWithAnd() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?search=database&status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Database issue"));
    }

    @Test
    void searchTermIsEscapedAgainstWildcards() throws Exception {
        ticketRepository.save(ticket("100% guarantee", "special chars here", Priority.MEDIUM, TicketStatus.OPEN,
                "2026-09-05T08:00:00Z", "2026-09-05T08:00:00Z"));
        // '%' inside search must match a literal '%', not act as a LIKE wildcard.
        mockMvc.perform(get("/api/v1/tickets").param("search", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("100% guarantee"));
    }

    @Test
    void ticketsSortedNewestFirst() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Closed case"))
                .andExpect(jsonPath("$.content[1].title").value("Login broken"))
                .andExpect(jsonPath("$.content[2].title").value("Database issue"));
    }

    @Test
    void listItemsOmitComments() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].comments").doesNotExist());
    }

    private Ticket ticket(String title, String description, Priority priority, TicketStatus status,
                          String createdAt, String updatedAt) {
        return new Ticket(UUID.randomUUID(), title, description, priority, status, "assignee",
                time(createdAt), time(updatedAt));
    }
}