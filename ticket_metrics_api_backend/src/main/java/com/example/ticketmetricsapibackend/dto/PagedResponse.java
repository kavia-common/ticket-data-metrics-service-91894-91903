package com.example.ticketmetricsapibackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

/**
 * PUBLIC_INTERFACE
 * Standard paginated response wrapper for API responses.
 *
 * @param <T> item type
 */
public class PagedResponse<T> {

    @Schema(description = "Zero-based page index", example = "0")
    private int page;

    @Schema(description = "Requested page size", example = "50")
    private int size;

    @Schema(description = "Total number of items across all pages", example = "123")
    private long totalItems;

    @Schema(description = "Total number of pages", example = "3")
    private int totalPages;

    @Schema(description = "List of items for the requested page")
    private List<T> items;

    public PagedResponse() {
    }

    public PagedResponse(int page, int size, long totalItems, int totalPages, List<T> items) {
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.items = (items == null) ? Collections.emptyList() : items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<T> getItems() {
        return items;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public void setItems(List<T> items) {
        this.items = (items == null) ? Collections.emptyList() : items;
    }
}
