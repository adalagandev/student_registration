package com.studentregistration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.studentregistration.dto.WaitlistEntryDto;
import com.studentregistration.service.WaitlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * WaitlistServiceTest — the parsing behaviour of {@link WaitlistService#listWaitlist()}.
 *
 * <p>Unlike the API suite, this is a PLAIN JUnit unit test: the service has no
 * injected dependencies, so we instantiate it directly with {@code new} and never
 * boot a Spring context (rule 8). It reads the REAL bundled
 * {@code src/main/resources/waitlist.txt} off the test classpath — that file is
 * static, read-only mock data (not the database), so reading it is deterministic
 * and correct, not an I/O isolation breach.
 */
// @agent: test-guardian
class WaitlistServiceTest {

    private final WaitlistService service = new WaitlistService();

    @Test
    void listWaitlistReturnsExactlySevenEntriesSkippingCommentsAndBlankLines() {
        // The file bundles 7 data rows plus 3 leading '#' comment lines. A count of
        // exactly 7 (not 10) proves the comment/blank lines were skipped, not parsed.
        List<WaitlistEntryDto> entries = service.listWaitlist();

        assertThat(entries).hasSize(7);
    }

    @Test
    void listWaitlistParsesTheFirstEntryFieldsAndTrimsColumnPadding() {
        // The source line pads columns with spaces for readability; the parsed values
        // must be trimmed. Record equality checks all four fields at once — one
        // logical assertion ("the first entry is parsed correctly").
        List<WaitlistEntryDto> entries = service.listWaitlist();

        assertThat(entries.get(0)).isEqualTo(new WaitlistEntryDto(
                "Ada Lovelace",
                "ada.lovelace@example.com",
                "Computer Science",
                "2026-05-02"));
    }

    @ParameterizedTest(name = "entry {index} has all four fields populated")
    @MethodSource("waitlistEntries")
    void everyEntryHasAllFourFieldsNonBlank(WaitlistEntryDto entry) {
        // One behaviour ("a returned entry is fully populated") across all seven rows;
        // each row reports as its own named test. isNotBlank() also rejects null, so
        // this covers the non-null AND non-blank requirement for every field.
        assertThat(entry.name()).isNotBlank();
        assertThat(entry.email()).isNotBlank();
        assertThat(entry.program()).isNotBlank();
        assertThat(entry.dateAdded()).isNotBlank();
    }

    /** Feeds the parameterized test the actual parsed entries (arrangement, not logic). */
    static List<WaitlistEntryDto> waitlistEntries() {
        return new WaitlistService().listWaitlist();
    }
}
