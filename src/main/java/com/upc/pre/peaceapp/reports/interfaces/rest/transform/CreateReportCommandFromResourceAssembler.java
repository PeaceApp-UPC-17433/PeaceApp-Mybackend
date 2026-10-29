package com.upc.pre.peaceapp.reports.interfaces.rest.transform;

import com.upc.pre.peaceapp.reports.domain.model.commands.CreateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.CreateReportResource;

public class CreateReportCommandFromResourceAssembler {

    public static CreateReportCommand toCommandFromResource(String requesterEmail, CreateReportResource resource) {
        return new CreateReportCommand(requesterEmail, resource.title(), resource.detail(), resource.type(),
                resource.severity(), resource.occurredAt(), GeoPoint.of(resource.latitude(), resource.longitude()),
                resource.address(), resource.image());
    }
}
