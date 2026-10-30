package com.upc.pre.peaceapp.alerts.interfaces.rest.transform;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.interfaces.rest.resources.AlertResource;

public class AlertResourceFromEntityAssembler {
    public static AlertResource toResourceFromEntity(Alert alert) {
        return new AlertResource(alert.getId(), alert.getReportId(), alert.getRecipientId(), alert.getReportTitle(),
                alert.getReportType(), alert.getReportSeverity(), alert.getReportLocation().latitude(),
                alert.getReportLocation().longitude(), alert.getDistanceMeters(), alert.getStatus(),
                alert.getCreatedAt().toInstant(), alert.getReadAt());
    }
}
