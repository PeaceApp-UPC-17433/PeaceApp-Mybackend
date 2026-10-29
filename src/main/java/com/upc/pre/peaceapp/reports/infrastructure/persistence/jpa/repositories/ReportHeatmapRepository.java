package com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.BoundingBox;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.HeatCell;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agrupa los reportes activos en una cuadrícula directamente en la base de datos.
 * Usa SQL nativo con el tamaño de celda como literal: MySQL no acepta un parámetro
 * en el GROUP BY si el SELECT repite la misma expresión con otro parámetro.
 * El literal sale de un {@code double} ya validado, así que no hay inyección posible.
 */
@Repository
public class ReportHeatmapRepository {
    private static final int MAX_CELLS = 2000;
    private static final String INACTIVE_STATUSES = Arrays.stream(ReportStatus.values())
            .filter(status -> !status.isActive())
            .map(status -> "'" + status.name() + "'")
            .collect(Collectors.joining(", "));

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<HeatCell> aggregate(BoundingBox box, double cellSizeDegrees, int minCount) {
        var size = BigDecimal.valueOf(cellSizeDegrees).toPlainString();
        var latCell = "FLOOR(latitude / " + size + ")";
        var lngCell = "FLOOR(longitude / " + size + ")";
        var sql = new StringBuilder("SELECT " + latCell + ", " + lngCell + ", COUNT(*) FROM reports WHERE status NOT IN (" + INACTIVE_STATUSES + ")");
        if (box != null)
            sql.append(" AND latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng");
        sql.append(" GROUP BY ").append(latCell).append(", ").append(lngCell)
                .append(" HAVING COUNT(*) >= :minCount ORDER BY COUNT(*) DESC");

        var query = entityManager.createNativeQuery(sql.toString()).setParameter("minCount", minCount);
        if (box != null) {
            query.setParameter("minLat", box.minLat()).setParameter("maxLat", box.maxLat())
                    .setParameter("minLng", box.minLng()).setParameter("maxLng", box.maxLng());
        }
        query.setMaxResults(MAX_CELLS);

        return ((List<Object[]>) query.getResultList()).stream()
                .map(row -> HeatCell.of(
                        (((Number) row[0]).doubleValue() + 0.5) * cellSizeDegrees,
                        (((Number) row[1]).doubleValue() + 0.5) * cellSizeDegrees,
                        ((Number) row[2]).longValue()))
                .toList();
    }
}
