package com.studentregistration;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import com.studentregistration.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

/**
 * StudentApiTest — locks the {@code /api/students} JSON contract (GET list, POST
 * create, PUT update) to Flask parity: exact status codes, camelCase field names and
 * exact error-message strings.
 */
class StudentApiTest extends ApiIntegrationTestBase {

    // ---- GET /api/students ---------------------------------------------------

    @Test
    void listReturnsStudentsNewestFirstWithCamelCaseFields() throws Exception {
        // Arrange: two students; the SECOND saved gets the higher id.
        Student first = persistStudent("Ann", "Lee", "ann@x.com", "CS", "1 St", "");
        Student second = persistStudent("Bob", "Ng", "bob@x.com", "Maths", "2 Ave", "555");

        // Act + Assert: ordered by id DESC (newest first), camelCase keys, no snake_case.
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(first.getId().intValue()))
                .andExpect(jsonPath("$[0].firstName").value("Bob"))
                .andExpect(jsonPath("$[0].lastName").value("Ng"))
                .andExpect(jsonPath("$[0].email").value("bob@x.com"))
                .andExpect(jsonPath("$[0].program").value("Maths"))
                .andExpect(jsonPath("$[0].address").value("2 Ave"))
                .andExpect(jsonPath("$[0].phone").value("555"))
                .andExpect(jsonPath("$[0].registeredAt").exists())
                // The DB uses snake_case; the API must NOT leak those keys.
                .andExpect(jsonPath("$[0].first_name").doesNotExist())
                .andExpect(jsonPath("$[0].registered_at").doesNotExist());
    }

    @Test
    void listReturnsEmptyArrayWhenNoStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    // ---- POST /api/students --------------------------------------------------

    @Test
    void createReturns201WithFullStudent() throws Exception {
        Map<String, String> body = validCreateBody();

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.program").value("Computer Science"))
                .andExpect(jsonPath("$.address").value("10 Main St"))
                .andExpect(jsonPath("$.phone").value("555-1234"));
    }

    @Test
    void createSetsServerRegisteredAtInIsoSecondsWithNoZoneSuffix() throws Exception {
        // The server owns registeredAt; the frontend parses "yyyy-MM-ddTHH:mm:ss"
        // with NO trailing Z/offset (Flask's isoformat(timespec="seconds")).
        Map<String, String> body = validCreateBody();

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registeredAt",
                        matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")));
    }

    @Test
    void createWithoutPhoneSucceedsAndPhoneDefaultsToEmptyString() throws Exception {
        // Phone is the one optional field: omitting it must still create the student.
        Map<String, String> body = validCreateBody();
        body.remove("phone");

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value(""));
    }

    @ParameterizedTest(name = "missing \"{0}\" -> 400")
    @ValueSource(strings = {"firstName", "lastName", "email", "program", "address"})
    void createMissingAnyRequiredFieldReturns400(String missingField) throws Exception {
        // One behaviour ("required fields are enforced") across the family of five
        // fields — each case reports as its own named test.
        Map<String, String> body = validCreateBody();
        body.remove(missingField);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("First name, last name, email, program and address are required."));
    }

    @Test
    void createTreatsWhitespaceOnlyRequiredFieldAsMissing() throws Exception {
        // Parity with Flask's (value or "").strip(): "   " counts as absent.
        Map<String, String> body = validCreateBody();
        body.put("firstName", "   ");

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("First name, last name, email, program and address are required."));
    }

    // ---- PUT /api/students/{id} ----------------------------------------------

    @Test
    void updateChangesOnlyContactFieldsLeavingNameAndProgramUntouched() throws Exception {
        // Arrange a student with a known name + program we expect to survive the update.
        Student student = persistStudent("Ann", "Lee", "old@x.com", "CS", "1 Old Rd", "111");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", "new@x.com");
        body.put("address", "2 New Ave");
        body.put("phone", "999");

        mockMvc.perform(put("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // Contact fields updated...
                .andExpect(jsonPath("$.email").value("new@x.com"))
                .andExpect(jsonPath("$.address").value("2 New Ave"))
                .andExpect(jsonPath("$.phone").value("999"))
                // ...name + program deliberately unchanged (program has its own endpoint).
                .andExpect(jsonPath("$.firstName").value("Ann"))
                .andExpect(jsonPath("$.lastName").value("Lee"))
                .andExpect(jsonPath("$.program").value("CS"));
    }

    @ParameterizedTest(name = "blank \"{0}\" -> 400")
    @ValueSource(strings = {"email", "address"})
    void updateWithBlankEmailOrAddressReturns400(String blankField) throws Exception {
        // Whitespace-only should be rejected exactly like empty (trim parity).
        // Validation runs before the existence check, so any id is fine here.
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", "valid@x.com");
        body.put("address", "1 Valid St");
        body.put("phone", "555");
        body.put(blankField, "   ");

        mockMvc.perform(put("/api/students/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email and address cannot be empty."));
    }

    @Test
    void updateUnknownStudentReturns404() throws Exception {
        // Valid body (so we pass validation) but a non-existent id.
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", "valid@x.com");
        body.put("address", "1 Valid St");
        body.put("phone", "555");

        mockMvc.perform(put("/api/students/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found."));
    }

    // --- Local arrangement helper --------------------------------------------

    /** A complete, valid create body; tests remove/override only the field they probe. */
    private static Map<String, String> validCreateBody() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", "Jane");
        body.put("lastName", "Doe");
        body.put("email", "jane@example.com");
        body.put("program", "Computer Science");
        body.put("address", "10 Main St");
        body.put("phone", "555-1234");
        return body;
    }
}
