package com.upc.pre.peaceapp.community.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;

import java.util.Map;

/**
 * Métricas de calidad. {@code verifiedRate} es verificados (incluye resueltos) sobre los que ya se revisaron
 * (verificados más descartados); {@code actionRate} es denuncias con sanción sobre las ya resueltas.
 * Ambas son nulas mientras no haya datos para calcularlas.
 */
public record CommunityMetricsResource(Reports reports, Community community, Flags flags) {
    public record Reports(long total, Map<ReportStatus, Long> byStatus, long verified, long discarded,
                          Double verifiedRate, Double averageAttentionHours) {
    }

    public record Community(long comments, long confirmations, long disputes) {
    }

    public record Flags(long open, long dismissed, long actionTaken, long total, Double actionRate) {
    }
}
