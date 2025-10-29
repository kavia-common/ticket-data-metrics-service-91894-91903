package com.example.ticketmetricsapibackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PUBLIC_INTERFACE
 * API response DTO for /api/tickets/metrics with exact key casing and value formatting.
 * Ensures:
 * - 'Month' is full month name (e.g., "September")
 * - Percentage fields are strings with trailing '%' (e.g., "95%")
 */
public class TicketMetricsApiResponseDTO {

    @JsonProperty("Application")
    @Schema(description = "Application name")
    private String application;

    @JsonProperty("Month")
    @Schema(description = "Full month name, e.g., 'September'")
    private String month;

    @JsonProperty("NoOfTicketsReceived")
    @Schema(description = "Total number of tickets received")
    private Integer noOfTicketsReceived;

    @JsonProperty("NoOfTicketsRespondedByTel")
    @Schema(description = "Number of tickets responded by telephone")
    private Integer noOfTicketsRespondedByTel;

    @JsonProperty("MTTRRespondMin")
    @Schema(description = "Mean time to respond in minutes")
    private Integer mttrRespondMin;

    @JsonProperty("AdherenceToResponseSLA")
    @Schema(description = "Adherence to response SLA, formatted like '95%'")
    private String adherenceToResponseSLA;

    @JsonProperty("SlippedResponseSLA")
    @Schema(description = "Number of responses that slipped SLA")
    private Integer slippedResponseSLA;

    @JsonProperty("ResponseAdherenceRate")
    @Schema(description = "Response adherence rate, formatted like '95%'")
    private String responseAdherenceRate;

    @JsonProperty("NoOfTicketsResolvedByTel")
    @Schema(description = "Number of tickets resolved by telephone")
    private Integer noOfTicketsResolvedByTel;

    @JsonProperty("MTTRResolveMin")
    @Schema(description = "Mean time to resolve in minutes")
    private Integer mttrResolveMin;

    @JsonProperty("AdherenceToResolutionSLA")
    @Schema(description = "Adherence to resolution SLA, formatted like '95%'")
    private String adherenceToResolutionSLA;

    @JsonProperty("SlippedResolutionSLA")
    @Schema(description = "Number of resolutions that slipped SLA")
    private Integer slippedResolutionSLA;

    @JsonProperty("ResolutionAdherenceRate")
    @Schema(description = "Resolution adherence rate, formatted like '95%'")
    private String resolutionAdherenceRate;

    @JsonProperty("Remarks")
    @Schema(description = "General remarks")
    private String remarks;

    @JsonProperty("ResolutionRemarks")
    @Schema(description = "Resolution-specific remarks")
    private String resolutionRemarks;

    public TicketMetricsApiResponseDTO() {}

    public TicketMetricsApiResponseDTO(
            String application,
            String month,
            Integer noOfTicketsReceived,
            Integer noOfTicketsRespondedByTel,
            Integer mttrRespondMin,
            String adherenceToResponseSLA,
            Integer slippedResponseSLA,
            String responseAdherenceRate,
            Integer noOfTicketsResolvedByTel,
            Integer mttrResolveMin,
            String adherenceToResolutionSLA,
            Integer slippedResolutionSLA,
            String resolutionAdherenceRate,
            String remarks,
            String resolutionRemarks) {
        this.application = application;
        this.month = month;
        this.noOfTicketsReceived = noOfTicketsReceived;
        this.noOfTicketsRespondedByTel = noOfTicketsRespondedByTel;
        this.mttrRespondMin = mttrRespondMin;
        this.adherenceToResponseSLA = adherenceToResponseSLA;
        this.slippedResponseSLA = slippedResponseSLA;
        this.responseAdherenceRate = responseAdherenceRate;
        this.noOfTicketsResolvedByTel = noOfTicketsResolvedByTel;
        this.mttrResolveMin = mttrResolveMin;
        this.adherenceToResolutionSLA = adherenceToResolutionSLA;
        this.slippedResolutionSLA = slippedResolutionSLA;
        this.resolutionAdherenceRate = resolutionAdherenceRate;
        this.remarks = remarks;
        this.resolutionRemarks = resolutionRemarks;
    }

    public String getApplication() {
        return application;
    }

    public String getMonth() {
        return month;
    }

    public Integer getNoOfTicketsReceived() {
        return noOfTicketsReceived;
    }

    public Integer getNoOfTicketsRespondedByTel() {
        return noOfTicketsRespondedByTel;
    }

    public Integer getMttrRespondMin() {
        return mttrRespondMin;
    }

    public String getAdherenceToResponseSLA() {
        return adherenceToResponseSLA;
    }

    public Integer getSlippedResponseSLA() {
        return slippedResponseSLA;
    }

    public String getResponseAdherenceRate() {
        return responseAdherenceRate;
    }

    public Integer getNoOfTicketsResolvedByTel() {
        return noOfTicketsResolvedByTel;
    }

    public Integer getMttrResolveMin() {
        return mttrResolveMin;
    }

    public String getAdherenceToResolutionSLA() {
        return adherenceToResolutionSLA;
    }

    public Integer getSlippedResolutionSLA() {
        return slippedResolutionSLA;
    }

    public String getResolutionAdherenceRate() {
        return resolutionAdherenceRate;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getResolutionRemarks() {
        return resolutionRemarks;
    }
}
