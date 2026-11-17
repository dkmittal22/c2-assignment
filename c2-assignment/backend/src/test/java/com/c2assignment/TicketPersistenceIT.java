package com.c2assignment;

import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import com.c2assignment.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-06 acceptance: entity round-trip against PostgreSQL, IDs are application-set
 * and enums are stored as strings (spec/test-strategy.md §5.4).
 */
@Transactional
class TicketPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAllTicketFieldsRoundTrip() {
        UUID appId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-24T06:00:00Z");
        Ticket ticket = new Ticket(appId, "title", "description", Priority.HIGH,
                TicketStatus.IN_PROGRESS, "alex", now, now);

        Ticket saved = ticketRepository.save(ticket);
        Optional<Ticket> loaded = ticketRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(appId);
        assertThat(loaded.get().getTitle()).isEqualTo("title");
        assertThat(loaded.get().getDescription()).isEqualTo("description");
        assertThat(loaded.get().getPriority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.get().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(loaded.get().getAssignee()).isEqualTo("alex");
        assertThat(loaded.get().getCreatedAt()).isEqualTo(now);
        assertThat(loaded.get().getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void storesEnumsAsStringsNotOrdinals() {
        UUID id = UUID.randomUUID();
        ticketRepository.saveAndFlush(new Ticket(
                id, "t", "d", Priority.CRITICAL, TicketStatus.RESOLVED, null,
                Instant.now(), Instant.now()));

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM tickets WHERE id = ?", String.class, id);
        String storedPriority = jdbcTemplate.queryForObject(
                "SELECT priority FROM tickets WHERE id = ?", String.class, id);

        assertThat(storedStatus).isEqualTo("RESOLVED");
        assertThat(storedPriority).isEqualTo("CRITICAL");
    }

    @Test
    void applicationAssignedUUIDIsPersisted() {
        UUID appId = UUID.randomUUID();
        Ticket saved = ticketRepository.save(new Ticket(
                appId, "t", "d", Priority.LOW, TicketStatus.OPEN, null, Instant.now(), Instant.now()));
        assertThat(saved.getId()).isEqualTo(appId);
        assertThat(ticketRepository.findById(appId)).isPresent();
    }
}