package com.upc.pre.peaceapp.alerts.domain.model.commands;

public record MarkAlertReadCommand(Long alertId, String requesterEmail) {
}
