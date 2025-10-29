package com.example.ticketmetricsapibackend.controller;

import com.example.ticketmetricsapibackend.dto.TicketMetricsResponse;
import com.example.ticketmetricsapibackend.service.TicketMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller that handles Excel file upload and returns computed ticket metrics.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Endpoints for uploading ticket Excel files and retrieving metrics")
public class TicketUploadController {

    private final TicketMetricsService ticketMetricsService;

    public TicketUploadController(TicketMetricsService ticketMetricsService) {
        this.ticketMetricsService = ticketMetricsService;
    }

    /**
     * PUBLIC_INTERFACE
     * POST /api/tickets/upload
     * Accepts a multipart/form-data Excel (.xlsx) file and returns computed metrics.
     *
     * @param file The Excel file under 'file' field.
     * @return TicketMetricsResponse JSON with computed metrics.
     */
    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Upload Excel and compute ticket metrics",
            description = "Consumes a .xlsx file with ticket records and returns metrics such as total ticket count, SLA adherence percentage, MTTR (hours), and remarks. " +
                    "Missing or differently-cased headers are tolerated; metrics are computed from available columns with sensible defaults. " +
                    "Only non-Excel uploads return 400, and only corrupt/unopenable files return 422.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metrics computed successfully",
                            content = @Content(schema = @Schema(implementation = TicketMetricsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request (missing/invalid file)",
                            content = @Content),
                    @ApiResponse(responseCode = "422", description = "Unprocessable Entity (unable to parse Excel)",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }
    )
    public ResponseEntity<TicketMetricsResponse> upload(@RequestPart("file") MultipartFile file) {
        TicketMetricsResponse response = ticketMetricsService.computeMetrics(file);
        return ResponseEntity.ok(response);
    }
}
