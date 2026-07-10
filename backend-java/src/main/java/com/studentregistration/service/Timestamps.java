package com.studentregistration.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Timestamps — produces the exact timestamp string format the Python backend used.
 *
 * <p>app.py wrote {@code datetime.utcnow().isoformat(timespec="seconds")}, which
 * yields e.g. {@code "2026-06-15T14:30:00"}: UTC, second precision, and crucially
 * NO trailing {@code Z} or timezone offset. The frontend parses this with
 * {@code new Date(...)}, so the shape must match.
 *
 * <p><b>Why an explicit formatter and not {@code LocalDateTime.toString()}?</b>
 * Java's {@code LocalDateTime.toString()} DROPS the seconds when they are zero
 * (e.g. {@code "2026-06-15T14:30"}), which would diverge from Python's always-present
 * seconds. A fixed pattern guarantees {@code HH:mm:ss} every time.
 */
public final class Timestamps {

    private static final DateTimeFormatter ISO_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Timestamps() {
        // Utility class — no instances.
    }

    /** Current UTC time as {@code yyyy-MM-ddTHH:mm:ss} (no offset). */
    public static String nowUtcSeconds() {
        return LocalDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(ISO_SECONDS);
    }
}
