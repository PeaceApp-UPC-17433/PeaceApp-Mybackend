package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

/** Área de búsqueda: un rectángulo o un círculo (centro y radio en metros). */
public record SearchArea(BoundingBox box, GeoPoint center, Double radiusMeters) {
    public static final double MAX_RADIUS_METERS = 50_000;

    public static SearchArea ofBox(BoundingBox box) {
        return new SearchArea(box, null, null);
    }

    public static SearchArea ofCircle(GeoPoint center, double radiusMeters) {
        if (!(radiusMeters > 0 && radiusMeters <= MAX_RADIUS_METERS))
            throw new IllegalArgumentException("El radio debe estar entre 1 y " + (int) MAX_RADIUS_METERS + " metros");
        return new SearchArea(BoundingBox.around(center, radiusMeters), center, radiusMeters);
    }

    public boolean contains(GeoPoint point) {
        return center == null ? box.contains(point) : center.distanceMetersTo(point) <= radiusMeters;
    }
}
