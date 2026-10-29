package com.upc.pre.peaceapp.reports.interfaces.rest.transform;

import com.upc.pre.peaceapp.reports.domain.model.commands.UpdateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.UpdateReportResource;

public class UpdateReportCommandFromResourceAssembler {

    public static UpdateReportCommand toCommandFromResource(Long reportId, String requesterEmail, boolean admin,
                                                            UpdateReportResource resource) {
        var location = resource.latitude() == null ? null : GeoPoint.of(resource.latitude(), resource.longitude());
        return new UpdateReportCommand(reportId, requesterEmail, admin, resource.title(), resource.detail(),
                resource.type(), resource.severity(), resource.occurredAt(), location, resource.address(),
                resource.image());
    }
}
