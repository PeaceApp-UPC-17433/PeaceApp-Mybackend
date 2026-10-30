package com.upc.pre.peaceapp.community.application.internal.commandservices;

import com.upc.pre.peaceapp.community.domain.model.commands.PurgeReportCommunityDataCommand;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.services.CommunityCleanupCommandService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.AbuseFlagRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportVoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommunityCleanupCommandServiceImpl implements CommunityCleanupCommandService {
    private final ReportCommentRepository commentRepository;
    private final ReportVoteRepository voteRepository;
    private final AbuseFlagRepository flagRepository;

    public CommunityCleanupCommandServiceImpl(ReportCommentRepository commentRepository,
                                              ReportVoteRepository voteRepository, AbuseFlagRepository flagRepository) {
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.flagRepository = flagRepository;
    }

    @Override
    public void handle(PurgeReportCommunityDataCommand command) {
        List<Long> commentIds = commentRepository.findIdsByReportId(command.reportId());
        if (!commentIds.isEmpty()) flagRepository.deleteByTargetTypeAndTargetIdIn(FlagTargetType.COMMENT, commentIds);
        flagRepository.deleteByTargetTypeAndTargetId(FlagTargetType.REPORT, command.reportId());
        commentRepository.deleteByReportId(command.reportId());
        voteRepository.deleteByReportId(command.reportId());
    }
}
