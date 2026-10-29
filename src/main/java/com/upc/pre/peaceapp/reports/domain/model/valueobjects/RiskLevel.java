package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

/** Nivel de riesgo de una zona según la cantidad de reportes activos. */
public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel fromCount(long activeReports) {
        if (activeReports >= 10) return CRITICAL;
        if (activeReports >= 6) return HIGH;
        if (activeReports >= 3) return MEDIUM;
        return LOW;
    }
}
