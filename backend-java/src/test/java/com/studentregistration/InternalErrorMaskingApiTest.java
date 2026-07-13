package com.studentregistration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studentregistration.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * InternalErrorMaskingApiTest — regression for SR-111 defect #1: an UNFORESEEN server
 * exception (a bug, a library blowing up) must be rendered as a generic
 * {@code {"error": "Internal server error."}} on HTTP 500, with the raw exception
 * message NEVER echoed to the client. An unhandled exception's message can carry SQL
 * fragments, file-system paths or library internals, so leaking it is an
 * information-disclosure defect.
 *
 * <p><b>Why standalone MockMvc and not the {@code @SpringBootTest} base:</b> the real
 * controllers are hard to force into an <em>unforeseen</em> 500 — every reachable
 * failure is already mapped to a specific handler (400/404/405/413/415). So instead of
 * contorting a real endpoint, we wire the REAL {@link GlobalExceptionHandler} as the
 * advice around a tiny throwaway controller whose sole job is to throw. This exercises
 * the exact catch-all {@code handleUnexpected} path in isolation, with no Spring context
 * to boot (rule 8), and lets us feed in a message we can then assert is masked.
 */
// @agent: test-guardian
class InternalErrorMaskingApiTest {

    /**
     * A stand-in for any endpoint whose implementation blows up unexpectedly. The message
     * carries a marker ("SECRET internal detail") we can assert never reaches the client.
     * {@code RuntimeException} is used precisely because it is NOT an {@code ApiException}
     * nor any of the framework exceptions with dedicated handlers, so it can only be
     * caught by the catch-all {@code handleUnexpected}.
     */
    @RestController
    static class BoomController {
        @GetMapping("/boom")
        String boom() {
            throw new RuntimeException("SECRET internal detail");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BoomController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void unexpectedExceptionReturns500WithGenericBodyAndNoRawMessageLeak() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                // The client sees ONLY the generic, fixed message...
                .andExpect(jsonPath("$.error").value("Internal server error."))
                // ...and the raw exception detail is NEVER echoed anywhere in the body.
                .andExpect(content().string(containsString("Internal server error.")))
                .andExpect(content().string(not(containsString("SECRET"))))
                // Still our shape, not Spring's default error JSON.
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }
}
