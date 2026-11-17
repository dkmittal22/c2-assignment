package com.c2assignment;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST API ITs for create / get / patch and error contract coverage
 * (spec/test-strategy.md §5.1, §5.5; api-contract §5.1/5.3/5.4).
 */
@Transactional
class TicketApiIT extends AbstractIntegrationTest {

    private static final String ISO8601 = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";

    @Test
    void createWithoutPriorityDefaultsToOpenAndMedium() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Cannot connect to database", "description": "Connection times out"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/tickets/[0-9a-f\\-]+")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Cannot connect to database"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value(matchesPattern(ISO8601)))
                .andExpect(jsonPath("$.updatedAt").value(matchesPattern(ISO8601)))
                .andExpect(jsonPath("$.comments").doesNotExist());
    }

    @Test
    void createWithPriorityAndAssignee() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Login broken", "description": "Cannot log in", "priority": "HIGH", "assignee": "alex"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee").value("alex"));
    }

    @Test
    void createRejectsBlankTitleWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   ", "description": "Desc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/tickets"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO8601)));
    }

    @Test
    void createRejectsBlankDescription() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Title", "description": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void createRejectsMissingRequireFields() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void createRejectsInvalidPriorityWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "T", "description": "D", "priority": "URGENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.priority").exists());
    }

    @Test
    void createRejectsTitleOverMaxLength() throws Exception {
        String title = "a".repeat(201);
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\", \"description\": \"D\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createRejectsAssigneeOverMaxLength() throws Exception {
        String assignee = "a".repeat(101);
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"T\", \"description\": \"D\", \"assignee\": \"" + assignee + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.assignee").exists());
    }

    @Test
    void createRejectsStatusFieldWithBadRequestAndNoRow() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "T", "description": "D", "status": "CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void createRejectsUnknownField() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "T", "description": "D", "bogus": 1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getExistingTicketReturnsCommentsArray() throws Exception {
        String id = createTicket("{\"title\": \"X\", \"description\": \"Y\"}");
        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("X"))
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void getMalformedUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getMissingTicketReturnsNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/tickets/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void patchUpdatesTitleAndRefreshesUpdatedAt() throws Exception {
        String id = createTicket("{\"title\": \"Old\", \"description\": \"Desc\"}");
        String oldUpdatedAt = fetchBody(id);

        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "New title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.comments").doesNotExist());

        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void patchWithStatusIsRejectedAndTicketUnchanged() throws Exception {
        String id = createTicket("{\"title\": \"Original\", \"description\": \"D\"}");

        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CLOSED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.title").value("Original"));
    }

    @Test
    void patchWithUnknownFieldIsRejected() throws Exception {
        String id = createTicket("{\"title\": \"T\", \"description\": \"D\"}");
        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nonsense": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void patchWithEmptyBodyIsRejected() throws Exception {
        String id = createTicket("{\"title\": \"T\", \"description\": \"D\"}");
        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void patchBlankTitleIsRejectedWithValidationError() throws Exception {
        String id = createTicket("{\"title\": \"T\", \"description\": \"D\"}");
        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void patchAssigneeOverMaxLengthIsRejected() throws Exception {
        String id = createTicket("{\"title\": \"T\", \"description\": \"D\"}");
        String assignee = "a".repeat(101);
        mockMvc.perform(patch("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\": \"" + assignee + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void patchMissingTicketReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "X"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void patchMalformedUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "X"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    private String createTicket(String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String fetchBody(String id) throws Exception {
        return mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}