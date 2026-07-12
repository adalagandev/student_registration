package com.studentregistration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * WaitlistApiTest — locks the {@code GET /api/waitlist} JSON contract the React
 * "waitlist" tab depends on: 200 status, a JSON array of the bundled entries, and
 * the exact camelCase field names Jackson emits from {@code WaitlistEntryDto}.
 *
 * <p>Extends {@link ApiIntegrationTestBase} so it runs through the real controller
 * and JSON serialization while the throwaway temp DB/uploads keep it off the shared
 * dev database — even though the waitlist itself never touches the DB.
 */
// @agent: test-guardian
class WaitlistApiTest extends ApiIntegrationTestBase {

    @Test
    void listReturnsSevenWaitlistEntriesAsAJsonArray() throws Exception {
        // The bundled waitlist.txt has 7 data rows (plus comment lines that are skipped).
        mockMvc.perform(get("/api/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(7)));
    }

    @Test
    void listExposesFirstEntryWithExactCamelCaseFieldNames() throws Exception {
        // The frontend reads {name, email, program, dateAdded}; assert those exact
        // camelCase keys and that no snake_case variant leaks through.
        mockMvc.perform(get("/api/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ada Lovelace"))
                .andExpect(jsonPath("$[0].email").value("ada.lovelace@example.com"))
                .andExpect(jsonPath("$[0].program").value("Computer Science"))
                .andExpect(jsonPath("$[0].dateAdded").value("2026-05-02"))
                // camelCase only — the snake_case form must NOT appear.
                .andExpect(jsonPath("$[0].date_added").doesNotExist());
    }
}
