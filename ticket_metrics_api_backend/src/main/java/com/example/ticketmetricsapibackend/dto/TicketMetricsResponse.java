package com.example.ticketmetricsapibackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * PUBLIC_INTERFACE
 * Represents the metrics computed from the uploaded tickets Excel file.
 */
public class TicketMetricsResponse {

    @Schema(description = "Total number of tickets parsed from the file")
    private int totalTickets;

    @Schema(description = "Percentage of tickets that met SLA, between 0 and 100")
    private double slaAdherencePercent;

    @Schema(description = "Mean Time To Resolution in hours")
    private double mttrHours;

    @Schema(description = "Processing remarks, including any warnings or validation notes")
    private String remarks;

    @Schema(description = "Optional detailed counters or extra info")
    private List<String> details;

    public TicketMetricsResponse() {}

    public TicketMetricsResponse(int totalTickets, double slaAdherencePercent, double mttrHours, String remarks, List<String> details) {
        this.totalTickets = totalTickets;
        this.slaAdherencePercent = slaAdherencePercent;
        this.mttrHours = mttrHours;
        this.remarks = remarks;
        this.details = details;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public double getSlaAdherencePercent() {
        return slaAdherencePercent;
    }

    public void setSlaAdherencePercent(double slaAdherencePercent) {
        this.slaAdherencePercent = slaAdherencePercent;
    }

    public double getMttrHours() {
        return mttrHours;
    }

    public void setMttrHours(double mttrHours) {
        this.mttrHours = mttrHours;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
