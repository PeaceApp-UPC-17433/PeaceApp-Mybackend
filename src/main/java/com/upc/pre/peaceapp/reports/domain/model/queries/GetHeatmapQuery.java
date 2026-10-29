package com.upc.pre.peaceapp.reports.domain.model.queries;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.BoundingBox;

/**
 * Mapa de calor por cuadrícula. {@code box} es opcional (sin él se cubre todo el mundo);
 * {@code cellSizeDegrees} es el lado de cada celda (0.005° son unos 550 m).
 */
public record GetHeatmapQuery(BoundingBox box, double cellSizeDegrees, int minCount) {
    public static final double MIN_CELL_SIZE = 0.0005;
    public static final double MAX_CELL_SIZE = 0.5;

    public GetHeatmapQuery {
        if (!(cellSizeDegrees >= MIN_CELL_SIZE && cellSizeDegrees <= MAX_CELL_SIZE))
            throw new IllegalArgumentException("El tamaño de celda debe estar entre " + MIN_CELL_SIZE + " y " + MAX_CELL_SIZE + " grados");
        if (minCount < 1) throw new IllegalArgumentException("minCount debe ser al menos 1");
    }
}
