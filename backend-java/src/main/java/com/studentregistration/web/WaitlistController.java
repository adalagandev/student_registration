package com.studentregistration.web;

import java.util.List;

import com.studentregistration.dto.WaitlistEntryDto;
import com.studentregistration.service.WaitlistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WaitlistController — exposes the read-only waitlist at {@code /api/waitlist}.
 *
 * <p>Like {@link StudentController}, this is intentionally THIN: it only maps the
 * HTTP route to a service call and lets Spring serialize the returned DTOs to JSON.
 * The waitlist is read-only for now, so this controller has a SINGLE {@code GET}
 * handler and no create/update/delete verbs. CORS for {@code /api/**} is already
 * configured in {@code config/CorsConfig}, so the Vite frontend can call this.
 */
@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /** GET /api/waitlist → 200 with a JSON array of waitlist entries. */
    @GetMapping
    public List<WaitlistEntryDto> list() {
        return waitlistService.listWaitlist();
    }
}
