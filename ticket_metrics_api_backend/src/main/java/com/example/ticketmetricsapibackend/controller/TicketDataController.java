package com.example.ticketmetricsapibackend.controller;

import com.example.ticketmetricsapibackend.dto.PagedResponse;
import com.example.ticketmetricsapibackend.dto.TicketRow;
import com.example.ticketmetricsapibackend.exception.BadRequestException;
import com.example.ticketmetricsapibackend.service.TicketMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/**
 * Controller exposing endpoints to retrieve parsed ticket rows.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Ticket Rows", description = "Endpoints to retrieve parsed rows from the last uploaded Excel")
public class TicketDataController {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    private final TicketMetricsService ticketMetricsService;

    public TicketDataController(TicketMetricsService ticketMetricsService) {
        this.ticketMetricsService = ticketMetricsService;
    }

    /**
     * PUBLIC_INTERFACE
     * GET /api/tickets/rows
     * Returns parsed rows from the last uploaded Excel with optional pagination and filtering.
     *
     * Query params:
     * - page: zero-based page index (default 0)
     * - size: page size (default 50)
     * - application: optional application filter (case-insensitive)
     * - month: optional month filter in yyyy-MM
     *
     * Returns 200 with empty items if no file has been uploaded yet.
     */
    @GetMapping(
            path = "/rows",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get parsed ticket rows",
            description = "Returns parsed rows from the most recently uploaded Excel file. Supports pagination via 'page' (0-based) and 'size' parameters, and optional filtering by 'application' and 'month' (yyyy-MM). Returns an empty items array if no file has been uploaded.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rows retrieved",
                            content = @Content(schema = @Schema(implementation = PagedResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
            }
    )
    public ResponseEntity<PagedResponse<TicketRow>> getRows(
            @Parameter(description = "Zero-based page index") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size (1..1000)") @RequestParam(name = "size", required = false, defaultValue = "50") Integer size,
            @Parameter(description = "Application name filter (case-insensitive)") @RequestParam(name = "application", required = false) String application,
            @Parameter(description = "Month in yyyy-MM format (e.g., 2024-07)") @RequestParam(name = "month", required = false) String month
    ) {

        // Validate page/size bounds
        if (page == null || page < 0) {
            throw new BadRequestException("Invalid 'page'. Must be >= 0.");
        }
        if (size == null || size < 1 || size > 1000) {
            throw new BadRequestException("Invalid 'size'. Must be within 1..1000.");
        }
        if (month != null && !month.isBlank() && !MONTH_PATTERN.matcher(month).matches()) {
            throw new BadRequestException("Invalid 'month' format. Expected yyyy-MM.");
        }

        PagedResponse<TicketRow> response = ticketMetricsService.getParsedRows(page, size, application, month);
        return ResponseEntity.ok(response);
    }
}
