package com.example.ticketmetricsapibackend.service;

import com.example.ticketmetricsapibackend.dto.TicketMetricEntry;
import com.example.ticketmetricsapibackend.dto.TicketMetricsResponse;
import com.example.ticketmetricsapibackend.exception.BadRequestException;
import com.example.ticketmetricsapibackend.exception.UnprocessableEntityException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * PUBLIC_INTERFACE
 * Service that processes an uploaded Excel file and computes ticket metrics.
 * Also provides retrieval of previously computed metrics with optional filtering.
 */
@Service
public class TicketMetricsService {

    // Simple in-memory storage for metrics entries to support GET filtering.
    // In a production system, this would be a repository/database.
    private final List<TicketMetricEntry> metricsStore = new ArrayList<>();

    // Expected columns (flexible: will try to find by header names)
    @Schema(description = "Logical header names expected in the uploaded XLSX")
    public enum Header {
        ID, CREATED_AT, RESOLVED_AT, PRIORITY, SLA_HOURS
    }

    /**
     * PUBLIC_INTERFACE
     * Parse and compute metrics from the provided Excel file.
     * Additionally stores an entry into the in-memory store without specific application/month context.
     * Clients may re-upload per application/month file if they need scoping.
     * @param file uploaded .xlsx file
     * @return TicketMetricsResponse with computed metrics
     */
    public TicketMetricsResponse computeMetrics(MultipartFile file) {
        validateFile(file);

        List<String> details = new ArrayList<>();
        int total = 0;
        int slaMet = 0;
        double totalResolutionHours = 0.0;
        int resolvedCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new UnprocessableEntityException("Excel has no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new UnprocessableEntityException("Excel has no data rows");
            }

            // Map headers
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            int colId = findColumnIndex(headerRow, "id");
            int colCreated = findColumnIndex(headerRow, "created_at", "created", "opened_at");
            int colResolved = findColumnIndex(headerRow, "resolved_at", "resolved", "closed_at");
            int colPriority = findColumnIndex(headerRow, "priority");
            int colSlaHours = findColumnIndex(headerRow, "sla_hours", "sla", "target_hours");

            if (colId == -1 || colCreated == -1) {
                throw new UnprocessableEntityException("Required headers missing: need at least id, created_at");
            }

            // Iterate rows
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                total++;

                LocalDateTime created = getDateCell(row.getCell(colCreated));
                LocalDateTime resolved = (colResolved != -1) ? getDateCell(row.getCell(colResolved)) : null;
                Double slaHours = (colSlaHours != -1) ? getNumericCell(row.getCell(colSlaHours)) : null;

                // MTTR
                if (created != null && resolved != null) {
                    double hours = Duration.between(created, resolved).toMinutes() / 60.0;
                    totalResolutionHours += Math.max(hours, 0.0);
                    resolvedCount++;
                }

                // SLA
                if (slaHours != null && created != null && resolved != null) {
                    double actual = Duration.between(created, resolved).toMinutes() / 60.0;
                    if (actual <= slaHours + 1e-9) {
                        slaMet++;
                    }
                }
            }

            double mttr = (resolvedCount > 0) ? (totalResolutionHours / resolvedCount) : 0.0;
            double slaPct = (total > 0) ? (slaMet * 100.0 / total) : 0.0;

            String remarks = buildRemarks(total, resolvedCount, slaPct);

            // Store a generic entry (no app/month context known at upload entrypoint)
            metricsStore.add(new TicketMetricEntry(null, null, total, round2(slaPct), round2(mttr)));

            return new TicketMetricsResponse(total, round2(slaPct), round2(mttr), remarks, details);

        } catch (UnprocessableEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new UnprocessableEntityException("Unable to parse Excel file", e);
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Returns metrics filtered by optional application and month.
     * If both are null, returns all entries. If no matches, returns an empty list.
     * @param application optional application name to filter (case-insensitive)
     * @param month optional month in yyyy-MM format
     * @return list of entries
     */
    public List<TicketMetricEntry> getMetrics(String application, String month) {
        List<TicketMetricEntry> filtered = new ArrayList<>();
        for (TicketMetricEntry entry : metricsStore) {
            if (application != null && !application.isBlank()) {
                String a = entry.getApplication();
                if (a == null || !a.equalsIgnoreCase(application)) {
                    continue;
                }
            }
            if (month != null && !month.isBlank()) {
                String m = entry.getMonth();
                if (m == null || !m.equals(month)) {
                    continue;
                }
            }
            filtered.add(entry);
        }
        return filtered;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and must not be empty");
        }
        String contentType = file.getContentType();
        String xlsx = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        // Some clients may send different but equivalent types; basic allow-list
        boolean ok = xlsx.equalsIgnoreCase(contentType)
                || "application/octet-stream".equalsIgnoreCase(contentType) // some proxies
                || "application/zip".equalsIgnoreCase(contentType); // some libs
        if (!ok) {
            throw new BadRequestException("Unsupported content type. Expecting " + xlsx);
        }
        // Size guard (example: 10 MB)
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File too large. Max 10 MB");
        }
        // Basic filename extension check
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("Only .xlsx files are supported");
        }
    }

    private int findColumnIndex(Row headerRow, String... expectedNames) {
        if (headerRow == null) return -1;
        for (Cell cell : headerRow) {
            String val = getStringCell(cell);
            if (val == null) continue;
            String norm = val.trim().toLowerCase(Locale.ROOT);
            for (String exp : expectedNames) {
                if (norm.equals(exp.toLowerCase(Locale.ROOT))) {
                    return cell.getColumnIndex();
                }
            }
        }
        return -1;
    }

    private String getStringCell(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> Double.toString(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield Double.toString(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        yield null;
                    }
                }
            }
            default -> null;
        };
    }

    private Double getNumericCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue();
                if (s == null || s.isBlank()) return null;
                return Double.parseDouble(s.trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                return cell.getNumericCellValue();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private LocalDateTime getDateCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault());
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue();
                if (s == null || s.isBlank()) return null;
                // Attempt parse as ISO-8601
                return LocalDateTime.parse(s.trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                try {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault());
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String buildRemarks(int total, int resolvedCount, double slaPct) {
        List<String> parts = new ArrayList<>();
        if (total == 0) {
            parts.add("No tickets found.");
        } else {
            if (resolvedCount < total) {
                parts.add((total - resolvedCount) + " ticket(s) without resolution date.");
            }
            if (slaPct < 80.0) {
                parts.add("SLA adherence below target.");
            } else {
                parts.add("SLA adherence acceptable.");
            }
        }
        return String.join(" ", parts);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
