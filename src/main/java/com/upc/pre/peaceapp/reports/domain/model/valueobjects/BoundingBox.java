package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

/** Rectángulo geográfico, normalmente el área visible del mapa. */
public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
    private static final double METERS_PER_DEGREE = 111_320;

    public BoundingBox {
        // Reutiliza la validación de rangos de GeoPoint
        GeoPoint.of(minLat, minLng);
        GeoPoint.of(maxLat, maxLng);
        if (minLat > maxLat) throw new IllegalArgumentException("minLat no puede ser mayor que maxLat");
        if (minLng > maxLng) throw new IllegalArgumentException("minLng no puede ser mayor que maxLng");
    }

    /** Caja que contiene al círculo; se usa para acotar la búsqueda antes de medir distancias exactas. */
    public static BoundingBox around(GeoPoint center, double radiusMeters) {
        var dLat = radiusMeters / METERS_PER_DEGREE;
        var cos = Math.max(Math.cos(Math.toRadians(center.latitude())), 0.01);
        var dLng = radiusMeters / (METERS_PER_DEGREE * cos);
        return new BoundingBox(
                Math.max(-90, center.latitude() - dLat), Math.min(90, center.latitude() + dLat),
                Math.max(-180, center.longitude() - dLng), Math.min(180, center.longitude() + dLng));
    }

    public boolean contains(GeoPoint point) {
        return point.latitude() >= minLat && point.latitude() <= maxLat
                && point.longitude() >= minLng && point.longitude() <= maxLng;
    }
}
