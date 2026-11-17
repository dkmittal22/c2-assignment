package com.c2assignment;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS verification for the local React frontend (spec/test-strategy.md §5.6,
 * api-contract §7, architecture §8).
 */
@Transactional
class CorsIT extends AbstractIntegrationTest {

    @Test
    void preflightFromFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/tickets")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }

    @Test
    void simpleGetFromFrontendOriginIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void disallowedOriginDoesNotReceiveCorsHeader() throws Exception {
        // Spring MVC rejects cross-origin requests from non-allowed origins with 403
        // and never includes the Access-Control-Allow-Origin header.
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}