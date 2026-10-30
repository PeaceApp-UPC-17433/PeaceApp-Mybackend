package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.commands.PurgeReportCommunityDataCommand;

public interface CommunityCleanupCommandService {
    /** Borra comentarios, votos y denuncias de un reporte que ya no existe. El historial de moderación se conserva. */
    void handle(PurgeReportCommunityDataCommand command);
}
