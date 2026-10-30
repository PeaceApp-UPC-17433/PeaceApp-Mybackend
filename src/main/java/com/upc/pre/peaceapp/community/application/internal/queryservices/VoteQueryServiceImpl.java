package com.upc.pre.peaceapp.community.application.internal.queryservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportVote;
import com.upc.pre.peaceapp.community.domain.model.queries.GetVoteSummaryQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteSummary;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import com.upc.pre.peaceapp.community.domain.services.VoteQueryService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportVoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VoteQueryServiceImpl implements VoteQueryService {
    private final ReportVoteRepository voteRepository;
    private final ExternalProfileService externalProfileService;
    private final ExternalReportService externalReportService;
    private final int threshold;

    public VoteQueryServiceImpl(ReportVoteRepository voteRepository, ExternalProfileService externalProfileService,
                                ExternalReportService externalReportService,
                                @Value("${community.verification.confirmation-threshold:3}") int threshold) {
        this.voteRepository = voteRepository;
        this.externalProfileService = externalProfileService;
        this.externalReportService = externalReportService;
        this.threshold = threshold;
    }

    @Override
    public VoteSummary handle(GetVoteSummaryQuery query) {
        var voterId = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        externalReportService.fetchReport(query.reportId()); // 404 si el reporte no existe
        var myVote = voteRepository.findByReportIdAndVoterId(query.reportId(), voterId).map(ReportVote::getType).orElse(null);
        return new VoteSummary(voteRepository.countByReportIdAndType(query.reportId(), VoteType.CONFIRM),
                voteRepository.countByReportIdAndType(query.reportId(), VoteType.DISPUTE), myVote, threshold);
    }
}
