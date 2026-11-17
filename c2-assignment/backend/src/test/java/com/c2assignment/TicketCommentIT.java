package com.c2assignment;

import com.c2assignment.comment.Comment;
import com.c2assignment.comment.CommentRepository;
import com.c2assignment.ticket.Priority;
import com.c2assignment.ticket.Ticket;
import com.c2assignment.ticket.TicketRepository;
import com.c2assignment.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
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
 * Comment API ITs (spec/test-strategy.md §5.2; api-contract §5.5, FR-5).
 */
@Transactional
class TicketCommentIT extends AbstractIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void addCommentToOpenTicketReturns201AndAppearsOnDetail() throws Exception {
        UUID id = seed("OPEN");

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "Looking into it"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("alex"))
                .andExpect(jsonPath("$.content").value("Looking into it"))
                .andExpect(jsonPath("$.ticketId").value(id.toString()))
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].author").value("alex"));
    }

    @Test
    void commentsAreReturnedChronologically() throws Exception {
        UUID id = seed("OPEN");
        commentRepository.save(comment(id, "first", "OLD 2026-09-01", Instant.parse("2026-09-01T00:00:00Z")));
        commentRepository.save(comment(id, "middle", "MID 2026-09-02", Instant.parse("2026-09-02T00:00:00Z")));
        commentRepository.save(comment(id, "last", "NEW 2026-09-03", Instant.parse("2026-09-03T00:00:00Z")));

        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(3))
                .andExpect(jsonPath("$.comments[0].content").value("OLD 2026-09-01"))
                .andExpect(jsonPath("$.comments[1].content").value("MID 2026-09-02"))
                .andExpect(jsonPath("$.comments[2].content").value("NEW 2026-09-03"));
    }

    @Test
    void blankContentReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void missingAuthorReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "hello"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.author").exists());
    }

    @Test
    void authorOverMaxLengthReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        String author = "a".repeat(101);
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\": \"" + author + "\", \"content\": \"hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void contentOverMaxLengthReturnsValidationError() throws Exception {
        UUID id = seed("OPEN");
        String content = "a".repeat(2001);
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\": \"alex\", \"content\": \"" + content + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownFieldReturnsBadRequest() throws Exception {
        UUID id = seed("OPEN");
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "hello", "rating": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void missingTicketReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "hello"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void malformedTicketUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/not-a-uuid/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "hello"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void commentAllowedOnClosedTicket() throws Exception {
        UUID id = seed("CLOSED");
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "follow-up note"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void commentAllowedOnCancelledTicket() throws Exception {
        UUID id = seed("CANCELLED");
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "why cancelled"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void parentUpdatedAtChangesAfterComment() throws Exception {
        UUID id = seed("OPEN");
        String before = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/tickets/{id}", id)).andReturn()
                        .getResponse().getContentAsString()).get("updatedAt").asText();

        mockMvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "alex", "content": "bump"}
                                """))
                .andExpect(status().isCreated());

        String after = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/tickets/{id}", id)).andReturn()
                        .getResponse().getContentAsString()).get("updatedAt").asText();

        // updatedAt changes on the parent ticket after a comment is added
        assertThat(after).isNotEqualTo(before);
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

    private Comment comment(UUID ticketId, String author, String content, Instant createdAt) {
        return new Comment(UUID.randomUUID(), ticketId, author, content, createdAt);
    }
}