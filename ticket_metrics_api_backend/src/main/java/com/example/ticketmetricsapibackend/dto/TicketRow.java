package com.example.ticketmetricsapibackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * PUBLIC_INTERFACE
 * Represents a single parsed row from the uploaded Excel file.
 * All fields are optional and defaulted where data is missing:
 * - numeric metrics default to 0
 * - text fields default to null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketRow {

    @Schema(description = "Ticket unique identifier if present in the Excel", example = "INC0012345")
    private String id;

    @Schema(description = "Ticket creation timestamp if present", example = "2024-07-09T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "Ticket resolution timestamp if present", example = "2024-07-10T12:00:00")
    private LocalDateTime resolvedAt;

    @Schema(description = "Application name if parsed", example = "AppA")
    private String application;

    @Schema(description = "Response time in minutes (derived if parsable)", example = "45")
    private int responseMinutes;

    @Schema(description = "Resolve/closure time in minutes (derived if parsable)", example = "180")
    private int resolveMinutes;

    @Schema(description = "Adherence to response SLA (0..100), defaults to 0 when unknown", example = "95")
    private int responseSlaPercent;

    @Schema(description = "Adherence to resolution SLA (0..100), defaults to 0 when unknown", example = "90")
    private int resolutionSlaPercent;

    @Schema(description = "Month in yyyy-MM derived from createdAt if present", example = "2024-07")
    private String month;

    public TicketRow() {}

    public TicketRow(
            String id,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt,
            String application,
            int responseMinutes,
            int resolveMinutes,
            int responseSlaPercent,
            int resolutionSlaPercent,
            String month
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.application = application;
        this.responseMinutes = responseMinutes;
        this.resolveMinutes = resolveMinutes;
        this.responseSlaPercent = responseSlaPercent;
        this.resolutionSlaPercent = resolutionSlaPercent;
        this.month = month;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getApplication() {
        return application;
    }

    public int getResponseMinutes() {
        return responseMinutes;
    }

    public int getResolveMinutes() {
        return resolveMinutes;
    }

    public int getResponseSlaPercent() {
        return responseSlaPercent;
    }

    public int getResolutionSlaPercent() {
        return resolutionSlaPercent;
    }

    public String getMonth() {
        return month;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setResponseMinutes(int responseMinutes) {
        this.responseMinutes = responseMinutes;
    }

    public void setResolveMinutes(int resolveMinutes) {
        this.resolveMinutes = resolveMinutes;
    }

    public void setResponseSlaPercent(int responseSlaPercent) {
        this.responseSlaPercent = responseSlaPercent;
    }

    public void setResolutionSlaPercent(int resolutionSlaPercent) {
        this.resolutionSlaPercent = resolutionSlaPercent;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
