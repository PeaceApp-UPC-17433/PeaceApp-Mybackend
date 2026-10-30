package com.upc.pre.peaceapp.community.application.internal.outboundservices.acl;

import com.upc.pre.peaceapp.reports.domain.model.commands.ChangeReportStatusCommand;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportByIdQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportQualityMetricsQuery;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportQualityMetrics;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import com.upc.pre.peaceapp.reports.domain.services.ReportQueryService;
import org.springframework.stereotype.Service;

/**
 * Capa anticorrupción hacia el contexto de reportes: comunidad ve una copia mínima del reporte
 * y le pide los cambios por sus servicios, nunca toca sus entidades ni su repositorio.
 */
@Service
public class ExternalReportService {
    /** Lo único que comunidad necesita saber de un reporte. */
    public record ReportSnapshot(Long id, Long reporterId, boolean active) {
    }

    private final ReportQueryService reportQueryService;
    private final ReportCommandService reportCommandService;

    public ExternalReportService(ReportQueryService reportQueryService, ReportCommandService reportCommandService) {
        this.reportQueryService = reportQueryService;
        this.reportCommandService = reportCommandService;
    }

    /** Lanza {@code ReportNotFoundException} si el reporte no existe. */
    public ReportSnapshot fetchReport(Long reportId) {
        var report = reportQueryService.handle(new GetReportByIdQuery(reportId));
        return new ReportSnapshot(report.getId(), report.getReporterId(), report.getStatus().isActive());
    }

    /** Descarta el reporte si sigue activo. Devuelve falso si ya estaba descartado o resuelto. */
    public boolean discardReport(Long reportId) {
        if (!fetchReport(reportId).active()) return false;
        reportCommandService.handle(new ChangeReportStatusCommand(reportId, true, ReportStatus.DISCARDED));
        return true;
    }

    public ReportQualityMetrics fetchQualityMetrics() {
        return reportQueryService.handle(new GetReportQualityMetricsQuery());
    }
}
