package com.example.ticketmetricsapibackend.controller;

import com.example.ticketmetricsapibackend.dto.TicketMetricEntry;
import com.example.ticketmetricsapibackend.dto.TicketMetricsApiResponseDTO;
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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller that exposes read endpoints for ticket metrics.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Ticket Metrics", description = "Endpoints to retrieve ticket metrics")
public class TicketMetricsController {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    private final TicketMetricsService ticketMetricsService;

    public TicketMetricsController(TicketMetricsService ticketMetricsService) {
        this.ticketMetricsService = ticketMetricsService;
    }

    /**
     * PUBLIC_INTERFACE
     * GET /api/tickets/metrics
     * Returns a JSON array of metrics filtered by optional query parameters: application and month (yyyy-MM).
     *
     * @param application optional application name filter
     * @param month optional month filter in yyyy-MM format
     * @return 200 OK with a JSON array (possibly empty)
     */
    @GetMapping(
            path = "/metrics",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get ticket metrics",
            description = "Returns a list of ticket metrics, optionally filtered by 'application' and 'month' (yyyy-MM). " +
                    "If no matches are found, returns an empty array.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metrics retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketMetricsApiResponseDTO.class)))),
                    @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
            }
    )
    public ResponseEntity<List<TicketMetricsApiResponseDTO>> getMetrics(
            @Parameter(description = "Application name to filter") @RequestParam(name = "application", required = false) String application,
            @Parameter(description = "Month in yyyy-MM format (e.g., 2024-07)") @RequestParam(name = "month", required = false) String month
    ) {
        if (month != null && !month.isBlank() && !MONTH_PATTERN.matcher(month).matches()) {
            throw new BadRequestException("Invalid 'month' format. Expected yyyy-MM.");
        }

        List<TicketMetricEntry> filtered = ticketMetricsService.getMetrics(application, month);
        List<TicketMetricsApiResponseDTO> result = filtered.stream()
                .map(ticketMetricsService::mapToApiDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
