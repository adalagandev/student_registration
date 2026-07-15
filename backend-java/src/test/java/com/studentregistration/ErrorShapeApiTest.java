package com.studentregistration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * ErrorShapeApiTest — guards the cross-cutting error-body INVARIANT: every failure the
 * frontend can hit returns {@code {"error": "..."}}, never Spring's default error JSON
 * ({@code {timestamp,status,error,message,path}}). The React client calls
 * {@code response.json()} and reads a top-level {@code error} string unconditionally,
 * so a shape drift here breaks error handling everywhere.
 */
class ErrorShapeApiTest extends ApiIntegrationTestBase {

    @Test
    void malformedJsonBodyReturns400WithOurErrorShapeNotSpringDefault() throws Exception {
        // A non-empty body Jackson cannot parse would, without the global handler,
        // surface as a leaky 500 in Spring's default shape.
        String malformed = "{ \"firstName\": ";

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest())
                // Our contract: a top-level "error" string...
                .andExpect(jsonPath("$.error").isString())
                // ...and NONE of Spring's default error keys.
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    /**
     * Regression for SR-111 defect #2: a URL no controller matches must come back as an
     * honest 404 in OUR error shape, not as a leaky 500 (the old behaviour, where the
     * unmatched route fell through to the catch-all) and not as Spring's default 404 page
     * ({@code {timestamp,status,error,message,path}}). {@code /api/bogus} is deliberately a
     * route no controller declares, so it exercises {@code handleNotFound}.
     */
    // @agent: test-warden
    @Test
    void unknownRouteReturns404WithOurNotFoundErrorShapeNotSpringDefault() throws Exception {
        mockMvc.perform(get("/api/bogus"))
                .andExpect(status().isNotFound())
                // Our contract: exactly {"error": "Not found."}...
                .andExpect(jsonPath("$.error").value("Not found."))
                // ...and NONE of Spring's default error keys.
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }
}
