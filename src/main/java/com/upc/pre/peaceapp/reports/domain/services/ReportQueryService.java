package com.upc.pre.peaceapp.reports.domain.services;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetHeatmapQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportByIdQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportsInAreaQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.SearchReportsQuery;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.HeatCell;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReportQueryService {
    Report handle(GetReportByIdQuery query);

    Page<Report> handle(SearchReportsQuery query);

    /** Reportes dentro del área visible, del más reciente al más antiguo y con un tope de resultados. */
    List<Report> handle(GetReportsInAreaQuery query);

    /** Solo cuenta reportes activos (ni descartados ni resueltos). */
    List<HeatCell> handle(GetHeatmapQuery query);
}
