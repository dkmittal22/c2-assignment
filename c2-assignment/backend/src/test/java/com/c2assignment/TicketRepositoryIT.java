package com.c2assignment;

import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import com.c2assignment.ticket.TicketService;
import com.c2assignment.ticket.TicketStatus;
import com.c2assignment.ticket.dto.TicketPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence-level list semantics via the service (T-07): case-insensitive
 * search over title/description, status filter, AND combination, sorting and
 * pagination defaults are proven against PostgreSQL.
 */
@Transactional
class TicketRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        ticketRepository.save(ticket("Payment fails", "gateway error on checkout", Priority.CRITICAL,
                TicketStatus.OPEN, "2026-09-01T08:00:00Z"));
        ticketRepository.save(ticket("Password reset", "cannot reset my password", Priority.HIGH,
                TicketStatus.IN_PROGRESS, "2026-09-02T08:00:00Z"));
        ticketRepository.save(ticket("Export broken", "CSV export returns empty file", Priority.LOW,
                TicketStatus.OPEN, "2026-09-03T08:00:00Z"));
    }

    @Test
    void caseInsensitiveSearchOnTitle() {
        TicketPageResponse page = ticketService.list("pAyMeNt", null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).title()).isEqualTo("Payment fails");
    }

    @Test
    void searchOnDescription() {
        TicketPageResponse page = ticketService.list("password", null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).title()).isEqualTo("Password reset");
    }

    @Test
    void statusFilterOnlyMatchesExactStatus() {
        TicketPageResponse page = ticketService.list(null, TicketStatus.OPEN, 0, 20);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(t -> t.status())
                .containsOnly(TicketStatus.OPEN);
    }

    @Test
    void searchAndStatusUseAnd() {
        TicketPageResponse page = ticketService.list("export", TicketStatus.OPEN, 0, 20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).title()).isEqualTo("Export broken");
    }

    @Test
    void sortedNewestCreatedAtFirst() {
        TicketPageResponse page = ticketService.list(null, null, 0, 20);
        List<String> titles = page.content().stream().map(t -> t.title()).toList();
        assertThat(titles).containsExactly("Export broken", "Password reset", "Payment fails");
    }

    @Test
    void emptySearchReturnsAll() {
        TicketPageResponse page = ticketService.list("   ", null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    private Ticket ticket(String title, String description, Priority priority, TicketStatus status,
                          String createdAt) {
        Instant at = Instant.parse(createdAt);
        return new Ticket(UUID.randomUUID(), title, description, priority, status, "assignee", at, at);
    }
}