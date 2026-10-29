package com.upc.pre.peaceapp.alerts.domain.model.commands;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.GeoPoint;

public record UpdateLastLocationCommand(String requesterEmail, GeoPoint location) {
}
