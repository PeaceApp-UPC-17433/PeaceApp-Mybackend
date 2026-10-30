package com.upc.pre.peaceapp.community.application.internal.queryservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import com.upc.pre.peaceapp.community.domain.model.queries.GetAbuseFlagsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommunityMetricsQuery;
import com.upc.pre.peaceapp.community.domain.model.queries.GetModerationLogQuery;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.CommunityMetrics;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import com.upc.pre.peaceapp.community.domain.services.ModerationQueryService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.AbuseFlagRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ModerationActionRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportVoteRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ModerationQueryServiceImpl implements ModerationQueryService {
    private final AbuseFlagRepository flagRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final ReportCommentRepository commentRepository;
    private final ReportVoteRepository voteRepository;
    private final ExternalReportService externalReportService;

    public ModerationQueryServiceImpl(AbuseFlagRepository flagRepository,
                                      ModerationActionRepository moderationActionRepository,
                                      ReportCommentRepository commentRepository, ReportVoteRepository voteRepository,
                                      ExternalReportService externalReportService) {
        this.flagRepository = flagRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.externalReportService = externalReportService;
    }

    @Override
    public Page<AbuseFlag> handle(GetAbuseFlagsQuery query) {
        requireAdmin(query.admin());
        return query.status() == null
                ? flagRepository.findAll(query.pageable())
                : flagRepository.findByStatus(query.status(), query.pageable());
    }

    @Override
    public Page<ModerationAction> handle(GetModerationLogQuery query) {
        requireAdmin(query.admin());
        return moderationActionRepository.findAll(query.pageable());
    }

    @Override
    public CommunityMetrics handle(GetCommunityMetricsQuery query) {
        requireAdmin(query.admin());
        return new CommunityMetrics(externalReportService.fetchQualityMetrics(), commentRepository.countByRemovedFalse(),
                voteRepository.countByType(VoteType.CONFIRM), voteRepository.countByType(VoteType.DISPUTE),
                flagRepository.countByStatus(FlagStatus.OPEN), flagRepository.countByStatus(FlagStatus.DISMISSED),
                flagRepository.countByStatus(FlagStatus.ACTION_TAKEN));
    }

    private static void requireAdmin(boolean admin) {
        if (!admin) throw new ForbiddenOperationException("Solo un administrador puede acceder al panel de moderación");
    }
}
