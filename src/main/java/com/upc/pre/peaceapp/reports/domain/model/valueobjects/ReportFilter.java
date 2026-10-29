package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

import java.time.Instant;

/** Criterios combinables para consultar reportes; los nulos no filtran. */
public record ReportFilter(ReportCategory category, ReportSeverity severity, ReportStatus status,
                           Instant from, Instant to) {
    public ReportFilter {
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
    }

    public static ReportFilter none() {
        return new ReportFilter(null, null, null, null, null);
    }
}
