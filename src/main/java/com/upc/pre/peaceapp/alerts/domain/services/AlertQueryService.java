package com.upc.pre.peaceapp.alerts.domain.services;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import com.upc.pre.peaceapp.alerts.domain.model.queries.*;
import org.springframework.data.domain.Page;

public interface AlertQueryService {
    Page<Alert> handle(GetAlertsQuery query);

    Page<Alert> handle(GetAlertsByUserQuery query);

    Alert handle(GetAlertByIdQuery query);

    long handle(CountUnreadAlertsQuery query);

    /** Si el usuario aún no configuró nada, devuelve los valores por defecto sin guardarlos. */
    AlertSettings handle(GetAlertSettingsQuery query);
}
