package com.example.ticketmetricsapibackend.service;

import com.example.ticketmetricsapibackend.dto.PagedResponse;
import com.example.ticketmetricsapibackend.dto.TicketMetricEntry;
import com.example.ticketmetricsapibackend.dto.TicketMetricsApiResponseDTO;
import com.example.ticketmetricsapibackend.dto.TicketMetricsResponse;
import com.example.ticketmetricsapibackend.dto.TicketRow;
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
import java.time.Month;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

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

    // Thread-safe in-memory store for the most recently parsed rows from upload
    private final List<TicketRow> lastParsedRows = new ArrayList<>();
    private final ReentrantReadWriteLock rowsLock = new ReentrantReadWriteLock();

    // Expected columns (flexible: will try to find by header names)
    @Schema(description = "Logical header names expected in the uploaded XLSX")
    public enum Header {
        ID, CREATED_AT, RESOLVED_AT, PRIORITY, SLA_HOURS, APPLICATION
    }

    /**
     * PUBLIC_INTERFACE
     * Parse and compute metrics from the provided Excel file.
     * Additionally stores an entry into the in-memory store without specific application/month context.
     * Clients may re-upload per application/month file if they need scoping.
     * Also persists parsed rows into an in-memory store for later retrieval.
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

        // For row persistence
        List<TicketRow> parsedRows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                // Truly corrupt/invalid Excel
                throw new UnprocessableEntityException("Excel has no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new UnprocessableEntityException("Unable to open first sheet");
            }
            // If there's only a header row or empty, treat as empty dataset -> defaults
            boolean hasAtLeastTwoRows = sheet.getPhysicalNumberOfRows() >= 2;

            // Map headers defensively (case-insensitive, tolerant to missing)
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            HeaderMap headerMap = mapHeaders(headerRow);

            if (!hasAtLeastTwoRows) {
                // No data rows, return defaults instead of 422
                String remarks = buildRemarks(0, 0, 0.0);
                metricsStore.add(new TicketMetricEntry(null, null, 0, 0.0, 0.0));

                // Persist empty rows list
                rowsLock.writeLock().lock();
                try {
                    lastParsedRows.clear();
                    lastParsedRows.addAll(parsedRows);
                } finally {
                    rowsLock.writeLock().unlock();
                }

                return new TicketMetricsResponse(0, 0.0, 0.0, remarks, details);
            }

            // Iterate rows
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String idStr = (headerMap.colId != -1) ? safeString(row.getCell(headerMap.colId)) : null;
                if (headerMap.colId != -1) {
                    if (idStr == null || idStr.isBlank()) {
                        details.add("Row " + (r + 1) + ": missing id");
                    }
                }

                LocalDateTime created = (headerMap.colCreated != -1) ? getDateCell(row.getCell(headerMap.colCreated)) : null;
                LocalDateTime resolved = (headerMap.colResolved != -1) ? getDateCell(row.getCell(headerMap.colResolved)) : null;
                Double slaHours = (headerMap.colSlaHours != -1) ? getNumericCell(row.getCell(headerMap.colSlaHours)) : null;
                String application = (headerMap.colApplication != -1) ? safeString(row.getCell(headerMap.colApplication)) : null;

                total++;

                // MTTR and resolve minutes
                double hours = 0.0;
                int resolveMinutes = 0;
                if (created != null && resolved != null) {
                    hours = Duration.between(created, resolved).toMinutes() / 60.0;
                    resolveMinutes = (int) Math.max(Math.round(Duration.between(created, resolved).toMinutes()), 0);
                    totalResolutionHours += Math.max(hours, 0.0);
                    resolvedCount++;
                }

                // SLA adherence
                boolean slaOk = false;
                if (slaHours != null && created != null && resolved != null) {
                    double actual = Duration.between(created, resolved).toMinutes() / 60.0;
                    if (actual <= slaHours + 1e-9) {
                        slaMet++;
                        slaOk = true;
                    }
                }

                // Response metrics: not strictly available; default 0 if unknown
                int responseMinutes = 0;
                int responseSlaPercent = 0;
                // Resolution metrics
                int resolutionSlaPercent = slaOk ? 100 : 0;

                String month = null;
                if (created != null) {
                    month = String.format("%04d-%02d", created.getYear(), created.getMonthValue());
                }

                TicketRow ticketRow = new TicketRow(
                        idStr,
                        created,
                        resolved,
                        application,
                        responseMinutes,
                        resolveMinutes,
                        responseSlaPercent,
                        resolutionSlaPercent,
                        month
                );
                parsedRows.add(ticketRow);
            }

            double mttr = (resolvedCount > 0) ? (totalResolutionHours / resolvedCount) : 0.0;
            double slaPct = (total > 0) ? (slaMet * 100.0 / total) : 0.0;

            String remarks = buildRemarks(total, resolvedCount, slaPct);

            // Store a generic entry (no app/month context known at upload entrypoint)
            metricsStore.add(new TicketMetricEntry(null, null, total, round2(slaPct), round2(mttr)));

            // Persist parsed rows atomically
            rowsLock.writeLock().lock();
            try {
                lastParsedRows.clear();
                lastParsedRows.addAll(parsedRows);
            } finally {
                rowsLock.writeLock().unlock();
            }

            return new TicketMetricsResponse(total, round2(slaPct), round2(mttr), remarks, details);

        } catch (UnprocessableEntityException e) {
            // Propagate known 422 conditions (e.g., corrupt workbook)
            throw e;
        } catch (Exception e) {
            // This captures corrupt files that cannot be opened at all
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

    /**
     * PUBLIC_INTERFACE
     * Returns parsed ticket rows from the last uploaded Excel, filtered by optional application and month
     * and paginated according to page/size.
     * If no file has been uploaded yet, returns an empty page with totalItems=0.
     *
     * @param page zero-based page index (>=0)
     * @param size page size (1..1000)
     * @param application optional application filter (case-insensitive)
     * @param month optional month in yyyy-MM
     * @return paged response with TicketRow items
     */
    public PagedResponse<TicketRow> getParsedRows(int page, int size, String application, String month) {
        List<TicketRow> snapshot;
        rowsLock.readLock().lock();
        try {
            snapshot = new ArrayList<>(lastParsedRows);
        } finally {
            rowsLock.readLock().unlock();
        }

        if (snapshot.isEmpty()) {
            return new PagedResponse<>(page, size, 0, 0, Collections.emptyList());
        }

        // Filter
        List<TicketRow> filtered = snapshot.stream()
                .filter(tr -> {
                    if (application != null && !application.isBlank()) {
                        String a = tr.getApplication();
                        if (a == null || !a.equalsIgnoreCase(application)) {
                            return false;
                        }
                    }
                    if (month != null && !month.isBlank()) {
                        String m = tr.getMonth();
                        if (m == null || !m.equals(month)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        if (total == 0) {
            return new PagedResponse<>(page, size, 0, 0, Collections.emptyList());
        }

        int totalPages = (int) Math.ceil(total / (double) size);
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<TicketRow> pageItems = filtered.subList(from, to);

        return new PagedResponse<>(page, size, total, totalPages, pageItems);
    }

    /**
     * PUBLIC_INTERFACE
     * Maps an internal TicketMetricEntry to the API response DTO enforcing the requested shape.
     * This is unit-test friendly and can be used by controllers/services.
     *
     * Month formatting rules:
     * - If entry.getMonth() provided in 'yyyy-MM', converts to full month name using English locale.
     * - If null/blank/unparseable, returns null for Month.
     *
     * Percentage fields are formatted as "<value>%", rounding to nearest integer for whole-number display.
     */
    public TicketMetricsApiResponseDTO mapToApiDto(TicketMetricEntry entry) {
        if (entry == null) return null;

        String application = entry.getApplication();

        String fullMonthName = null;
        String ym = entry.getMonth();
        if (ym != null && ym.matches("^[0-9]{4}-(0[1-9]|1[0-2])$")) {
            int monthNumber = Integer.parseInt(ym.substring(5, 7)); // 01..12
            Month m = Month.of(monthNumber);
            fullMonthName = m.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        }

        // Map available values; use defaults for missing parts.
        Integer noOfTicketsReceived = entry.getTotalTickets();
        Integer noOfTicketsRespondedByTel = null;
        Integer mttrRespondMin = null;

        String adherenceToResponseSLA = null;
        Integer slippedResponseSLA = null;
        String responseAdherenceRate = null;

        Integer noOfTicketsResolvedByTel = null;

        Integer mttrResolveMin = (int) Math.round(entry.getMttrHours() * 60.0);

        String adherenceToResolutionSLA = formatPercent(entry.getSlaAdherencePercent());
        String resolutionAdherenceRate = formatPercent(entry.getSlaAdherencePercent());

        String remarks = null;
        String resolutionRemarks = null;

        return new TicketMetricsApiResponseDTO(
                application,
                fullMonthName,
                noOfTicketsReceived,
                noOfTicketsRespondedByTel,
                mttrRespondMin,
                adherenceToResponseSLA,
                slippedResponseSLA,
                responseAdherenceRate,
                noOfTicketsResolvedByTel,
                mttrResolveMin,
                adherenceToResolutionSLA,
                null,
                resolutionAdherenceRate,
                remarks,
                resolutionRemarks
        );
    }

    private String formatPercent(double value) {
        long rounded = Math.round(value);
        return rounded + "%";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and must not be empty");
        }
        String contentType = file.getContentType();
        String xlsx = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        boolean ok = xlsx.equalsIgnoreCase(contentType)
                || "application/octet-stream".equalsIgnoreCase(contentType)
                || "application/zip".equalsIgnoreCase(contentType);
        if (!ok) {
            throw new BadRequestException("Unsupported content type. Expecting " + xlsx);
        }
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File too large. Max 10 MB");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("Only .xlsx files are supported");
        }
    }

    /**
     * Map header names to indices with case-insensitive matching and flexible aliases.
     * Missing headers are tolerated and represented by -1.
     */
    private HeaderMap mapHeaders(Row headerRow) {
        HeaderMap hm = new HeaderMap();
        if (headerRow == null) {
            // No headers at all; everything remains -1
            return hm;
        }

        Map<String, Integer> byName = new HashMap<>();
        for (Cell cell : headerRow) {
            String s = getStringCell(cell);
            if (s == null) continue;
            byName.put(s.trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
        }

        hm.colId = findByAliases(byName, "id", "ticket_id", "issue_id", "sr_no", "sr", "no", "number");
        hm.colCreated = findByAliases(byName, "created_at", "created", "opened_at", "open_date", "created date", "date created");
        hm.colResolved = findByAliases(byName, "resolved_at", "resolved", "closed_at", "close_date", "resolved date", "date resolved");
        hm.colPriority = findByAliases(byName, "priority", "prio", "severity", "impact");
        hm.colSlaHours = findByAliases(byName, "sla_hours", "sla", "target_hours", "target", "resolution_target_hours");
        hm.colApplication = findByAliases(byName, "application", "app", "service", "project");

        return hm;
    }

    private int findByAliases(Map<String, Integer> map, String... aliases) {
        for (String a : aliases) {
            Integer idx = map.get(a.toLowerCase(Locale.ROOT));
            if (idx != null) return idx;
        }
        return -1;
    }

    private String safeString(Cell cell) {
        String s = getStringCell(cell);
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static class HeaderMap {
        int colId = -1;
        int colCreated = -1;
        int colResolved = -1;
        int colPriority = -1;
        int colSlaHours = -1;
        int colApplication = -1;
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
                return Double.parseDouble(s.trim().replaceAll(",", ""));
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
                // Attempt multiple parses: ISO-8601 or Excel-like "yyyy-MM-dd HH:mm[:ss]"
                String trimmed = s.trim();
                try {
                    return LocalDateTime.parse(trimmed);
                } catch (Exception ignoreIso) {
                    // Try a couple of common patterns without bringing new dependencies
                    // Simple fallback: if date-only pattern "yyyy-MM-dd"
                    if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return LocalDateTime.parse(trimmed + "T00:00:00");
                    }
                }
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
