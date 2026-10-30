package com.upc.pre.peaceapp.reports.domain.services;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.commands.ChangeReportStatusCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.CreateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.DeleteReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.UpdateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.VerifyReportByCommunityCommand;

public interface ReportCommandService {
    Report handle(CreateReportCommand command);

    /** Solo el dueño del reporte o un administrador pueden editarlo. */
    Report handle(UpdateReportCommand command);

    /** Solo un administrador puede cambiar el estado. */
    Report handle(ChangeReportStatusCommand command);

    /** Orden del sistema: verifica el reporte si todavía está pendiente o en revisión. Un reporte ausente se ignora. */
    void handle(VerifyReportByCommunityCommand command);

    /** Solo el dueño del reporte o un administrador pueden eliminarlo. */
    void handle(DeleteReportCommand command);
}
