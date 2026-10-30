package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

import java.util.Map;

/**
 * Indicadores de calidad de los reportes.
 *
 * @param countByStatus         cantidad de reportes por estado (todos los estados aparecen, incluso con cero)
 * @param averageAttentionHours horas promedio entre la creación y el último cambio de los reportes ya atendidos
 *                              (verificados, descartados o resueltos); nulo si aún no hay ninguno
 */
public record ReportQualityMetrics(Map<ReportStatus, Long> countByStatus, Double averageAttentionHours) {
    public long total() {
        return countByStatus.values().stream().mapToLong(Long::longValue).sum();
    }

    /** Verificados incluye los ya resueltos, porque para ser resueltos pasaron por verificación. */
    public long verified() {
        return countByStatus.getOrDefault(ReportStatus.VERIFIED, 0L) + countByStatus.getOrDefault(ReportStatus.RESOLVED, 0L);
    }

    public long discarded() {
        return countByStatus.getOrDefault(ReportStatus.DISCARDED, 0L);
    }
}
