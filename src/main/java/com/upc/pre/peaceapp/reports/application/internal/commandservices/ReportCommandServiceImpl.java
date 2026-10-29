package com.upc.pre.peaceapp.reports.application.internal.commandservices;

import com.upc.pre.peaceapp.reports.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.reports.domain.exceptions.ReportNotFoundException;
import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.commands.ChangeReportStatusCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.CreateReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.DeleteReportCommand;
import com.upc.pre.peaceapp.reports.domain.model.commands.UpdateReportCommand;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories.ReportRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {
    private final ReportRepository reportRepository;
    private final ExternalProfileService externalProfileService;

    public ReportCommandServiceImpl(ReportRepository reportRepository, ExternalProfileService externalProfileService) {
        this.reportRepository = reportRepository;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public Report handle(CreateReportCommand command) {
        var reporterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        return reportRepository.save(new Report(reporterId, command.title(), command.detail(), command.category(),
                command.severity(), command.occurredAt(), command.location(), command.address(), command.image()));
    }

    @Override
    public Report handle(UpdateReportCommand command) {
        var report = findReport(command.reportId());
        assertCanManage(report, command.requesterEmail(), command.admin());
        report.edit(command.title(), command.detail(), command.category(), command.severity(),
                command.occurredAt(), command.location(), command.address(), command.image());
        return reportRepository.save(report);
    }

    @Override
    public Report handle(ChangeReportStatusCommand command) {
        if (!command.admin())
            throw new ForbiddenOperationException("Solo un administrador puede cambiar el estado de un reporte");
        var report = findReport(command.reportId());
        report.changeStatus(command.newStatus());
        return reportRepository.save(report);
    }

    @Override
    public void handle(DeleteReportCommand command) {
        var report = findReport(command.reportId());
        assertCanManage(report, command.requesterEmail(), command.admin());
        reportRepository.delete(report);
    }

    private Report findReport(Long id) {
        return reportRepository.findById(id).orElseThrow(ReportNotFoundException::new);
    }

    private void assertCanManage(Report report, String requesterEmail, boolean admin) {
        if (admin) return;
        var requesterId = externalProfileService.fetchProfileIdByEmail(requesterEmail);
        if (!report.isOwnedBy(requesterId))
            throw new ForbiddenOperationException("No tienes permiso para modificar este reporte");
    }
}
