package com.upc.pre.peaceapp.reports.domain.model.queries;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportFilter;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.SearchArea;

public record GetReportsInAreaQuery(SearchArea area, ReportFilter filter, int limit) {
}
