package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.RiskLevel;

public record HeatCellResource(double latitude, double longitude, long count, RiskLevel riskLevel) {
}
