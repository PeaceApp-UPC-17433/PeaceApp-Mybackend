package com.upc.pre.peaceapp.alerts.domain.model.commands;

public record DeleteAlertCommand(Long alertId, String requesterEmail) {
}
