package com.example.ticketmetricsapibackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;

/**
 * PUBLIC_INTERFACE
 * Represents a single metric entry for tickets, optionally scoped by application and month.
 */
public class TicketMetricEntry {

    @Schema(description = "Application name for which this metric is computed", example = "AppA")
    private String application;

    @Schema(description = "Month in yyyy-MM format for which this metric applies", example = "2024-07")
    private String month;

    @Schema(description = "Total number of tickets")
    private int totalTickets;

    @Schema(description = "SLA adherence percentage (0..100)")
    private double slaAdherencePercent;

    @Schema(description = "Mean Time To Resolution (hours)")
    private double mttrHours;

    public TicketMetricEntry() {
    }

    public TicketMetricEntry(String application, String month, int totalTickets, double slaAdherencePercent, double mttrHours) {
        this.application = application;
        this.month = month;
        this.totalTickets = totalTickets;
        this.slaAdherencePercent = slaAdherencePercent;
        this.mttrHours = mttrHours;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getMonth() {
        return month;
    }

    /**
     * Sets the month in yyyy-MM format.
     */
    public void setMonth(String month) {
        this.month = month;
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
}
