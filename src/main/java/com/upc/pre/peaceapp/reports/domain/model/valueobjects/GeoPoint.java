package com.upc.pre.peaceapp.reports.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Coordenada geográfica válida (WGS84). */
@Embeddable
public record GeoPoint(
        @Column(name = "latitude", nullable = false) double latitude,
        @Column(name = "longitude", nullable = false) double longitude) {
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public GeoPoint {
        if (!(latitude >= -90 && latitude <= 90))
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
        if (!(longitude >= -180 && longitude <= 180))
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
    }

    public static GeoPoint of(double latitude, double longitude) {
        return new GeoPoint(latitude, longitude);
    }

    /** Distancia en metros por la fórmula de Haversine. */
    public double distanceMetersTo(GeoPoint other) {
        var dLat = Math.toRadians(other.latitude - latitude);
        var dLng = Math.toRadians(other.longitude - longitude);
        var a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }
}
