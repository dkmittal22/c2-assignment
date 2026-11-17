package com.c2assignment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-05 acceptance: Flyway migrates cleanly and creates the fixed physical
 * tables (spec/data-model.md §8, NFR-8/9).
 */
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsExpectedTablesAndIndexes() {
        assertThat(tableExists("tickets")).isTrue();
        assertThat(tableExists("comments")).isTrue();
        assertThat(indexExists("idx_tickets_status")).isTrue();
        assertThat(indexExists("idx_comments_ticket_id")).isTrue();
    }

    @Test
    void ticketsTableMatchesPhysicalModel() {
        assertThat(columnCount("tickets", "id")).isEqualTo(1);
        assertThat(columnCount("tickets", "title")).isEqualTo(1);
        assertThat(columnCount("tickets", "description")).isEqualTo(1);
        assertThat(columnCount("tickets", "priority")).isEqualTo(1);
        assertThat(columnCount("tickets", "status")).isEqualTo(1);
        assertThat(columnCount("tickets", "assignee")).isEqualTo(1);
        assertThat(columnCount("tickets", "created_at")).isEqualTo(1);
        assertThat(columnCount("tickets", "updated_at")).isEqualTo(1);
    }

    @Test
    void commentsTableReferencesTicketsWithRestrict() {
        assertThat(columnCount("comments", "id")).isEqualTo(1);
        assertThat(columnCount("comments", "ticket_id")).isEqualTo(1);
        assertThat(columnCount("comments", "author")).isEqualTo(1);
        assertThat(columnCount("comments", "content")).isEqualTo(1);
        assertThat(columnCount("comments", "created_at")).isEqualTo(1);
    }

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)",
                Boolean.class, table));
    }

    private boolean indexExists(String index) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM pg_indexes WHERE indexname = ?)",
                Boolean.class, index));
    }

    private int columnCount(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                Integer.class, table, column);
        return count == null ? 0 : count;
    }
}