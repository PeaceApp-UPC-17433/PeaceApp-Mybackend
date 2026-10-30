package com.upc.pre.peaceapp.alerts.domain.services;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.domain.model.commands.*;

public interface AlertCommandService {
    /** Crea una alerta por cada usuario cercano al reporte; devuelve cuántas creó. */
    int handle(GenerateProximityAlertsCommand command);

    Alert handle(MarkAlertReadCommand command);

    /** Devuelve cuántas alertas pasaron a leídas. */
    int handle(MarkAllAlertsReadCommand command);

    void handle(DeleteAlertCommand command);

    void handle(DeleteMyAlertsCommand command);

    AlertSettings handle(UpdateAlertSettingsCommand command);

    AlertSettings handle(UpdateLastLocationCommand command);
}
