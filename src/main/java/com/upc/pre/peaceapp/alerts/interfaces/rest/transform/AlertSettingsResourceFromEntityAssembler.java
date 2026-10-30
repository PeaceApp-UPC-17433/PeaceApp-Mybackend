package com.upc.pre.peaceapp.alerts.interfaces.rest.transform;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.interfaces.rest.resources.AlertSettingsResource;

public class AlertSettingsResourceFromEntityAssembler {
    public static AlertSettingsResource toResourceFromEntity(AlertSettings settings) {
        var location = settings.getLastLocation();
        return new AlertSettingsResource(settings.getRadiusMeters(), settings.getMinSeverity(),
                settings.getIgnoredTypes(), settings.getMutedUntil(),
                location == null ? null : location.latitude(), location == null ? null : location.longitude(),
                settings.getLocationUpdatedAt());
    }
}
