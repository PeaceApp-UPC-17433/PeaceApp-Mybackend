package com.upc.pre.peaceapp.community.application.internal.commandservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportVote;
import com.upc.pre.peaceapp.community.domain.model.commands.CastVoteCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.RetractVoteCommand;
import com.upc.pre.peaceapp.community.domain.model.events.ReportConfirmedByCommunityEvent;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteSummary;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import com.upc.pre.peaceapp.community.domain.services.VoteCommandService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportVoteRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoteCommandServiceImpl implements VoteCommandService {
    private final ReportVoteRepository voteRepository;
    private final ExternalProfileService externalProfileService;
    private final ExternalReportService externalReportService;
    private final ApplicationEventPublisher eventPublisher;
    private final int threshold;

    public VoteCommandServiceImpl(ReportVoteRepository voteRepository, ExternalProfileService externalProfileService,
                                  ExternalReportService externalReportService, ApplicationEventPublisher eventPublisher,
                                  @Value("${community.verification.confirmation-threshold:3}") int threshold) {
        if (threshold < 1) throw new IllegalArgumentException("El umbral de verificación debe ser al menos 1");
        this.voteRepository = voteRepository;
        this.externalProfileService = externalProfileService;
        this.externalReportService = externalReportService;
        this.eventPublisher = eventPublisher;
        this.threshold = threshold;
    }

    @Override
    public VoteSummary handle(CastVoteCommand command) {
        var voterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var report = externalReportService.fetchReport(command.reportId());
        if (report.reporterId().equals(voterId))
            throw new ForbiddenOperationException("No puedes votar tu propio reporte");
        if (!report.active()) throw new IllegalArgumentException("El reporte ya no admite votos");

        voteRepository.findByReportIdAndVoterId(command.reportId(), voterId).ifPresentOrElse(
                existing -> {
                    existing.changeTo(command.type());
                    voteRepository.save(existing);
                },
                () -> voteRepository.save(new ReportVote(command.reportId(), voterId, command.type())));

        var summary = summarize(command.reportId(), voterId);
        if (command.type() == VoteType.CONFIRM && summary.reachedThreshold())
            eventPublisher.publishEvent(new ReportConfirmedByCommunityEvent(command.reportId(), summary.net()));
        return summary;
    }

    @Override
    public VoteSummary handle(RetractVoteCommand command) {
        var voterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        externalReportService.fetchReport(command.reportId()); // 404 si el reporte no existe
        voteRepository.findByReportIdAndVoterId(command.reportId(), voterId).ifPresent(voteRepository::delete);
        return summarize(command.reportId(), voterId);
    }

    private VoteSummary summarize(Long reportId, Long voterId) {
        voteRepository.flush();
        var myVote = voteRepository.findByReportIdAndVoterId(reportId, voterId).map(ReportVote::getType).orElse(null);
        return new VoteSummary(voteRepository.countByReportIdAndType(reportId, VoteType.CONFIRM),
                voteRepository.countByReportIdAndType(reportId, VoteType.DISPUTE), myVote, threshold);
    }
}
