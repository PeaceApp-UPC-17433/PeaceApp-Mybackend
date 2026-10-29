package com.upc.pre.peaceapp.reports.interfaces.rest.transform;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.*;

public class ReportResourceFromEntityAssembler {

    public static ReportResource toResourceFromEntity(Report report) {
        return new ReportResource(report.getId(), report.getTitle(), report.getDetail(), report.getCategory(),
                report.getSeverity(), report.getStatus(), report.getOccurredAt(), report.getLocation().latitude(),
                report.getLocation().longitude(), report.getAddress(), report.getImage(), report.getReporterId(),
                report.getCreatedAt(), report.getUpdatedAt());
    }

    public static MapPointResource toMapPointFromEntity(Report report) {
        return new MapPointResource(report.getId(), report.getLocation().latitude(), report.getLocation().longitude(),
                report.getCategory(), report.getSeverity());
    }

    public static LocationResource toLocationFromEntity(Report report) {
        return new LocationResource(report.getId(), report.getLocation().latitude(), report.getLocation().longitude(),
                report.getId());
    }
}
