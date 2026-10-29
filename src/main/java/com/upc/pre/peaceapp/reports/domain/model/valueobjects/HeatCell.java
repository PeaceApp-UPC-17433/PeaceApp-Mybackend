package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

/** Celda del mapa de calor: centro de la celda, reportes activos y nivel de riesgo. */
public record HeatCell(double latitude, double longitude, long count, RiskLevel riskLevel) {
    public static HeatCell of(double latitude, double longitude, long count) {
        return new HeatCell(latitude, longitude, count, RiskLevel.fromCount(count));
    }
}
