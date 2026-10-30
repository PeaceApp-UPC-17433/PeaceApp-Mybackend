package com.upc.pre.peaceapp.community.application.internal.commandservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.exceptions.AbuseFlagNotFoundException;
import com.upc.pre.peaceapp.community.domain.exceptions.CommentNotFoundException;
import com.upc.pre.peaceapp.community.domain.exceptions.DuplicateAbuseFlagException;
import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import com.upc.pre.peaceapp.community.domain.model.commands.FlagContentCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.ResolveAbuseFlagCommand;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.ModerationActionType;
import com.upc.pre.peaceapp.community.domain.services.ModerationCommandService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.AbuseFlagRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ModerationActionRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ModerationCommandServiceImpl implements ModerationCommandService {
    private final AbuseFlagRepository flagRepository;
    private final ReportCommentRepository commentRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final ExternalProfileService externalProfileService;
    private final ExternalReportService externalReportService;

    public ModerationCommandServiceImpl(AbuseFlagRepository flagRepository, ReportCommentRepository commentRepository,
                                        ModerationActionRepository moderationActionRepository,
                                        ExternalProfileService externalProfileService,
                                        ExternalReportService externalReportService) {
        this.flagRepository = flagRepository;
        this.commentRepository = commentRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.externalProfileService = externalProfileService;
        this.externalReportService = externalReportService;
    }

    @Override
    public AbuseFlag handle(FlagContentCommand command) {
        var reporterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var ownerId = switch (command.targetType()) {
            case REPORT -> externalReportService.fetchReport(command.targetId()).reporterId();
            case COMMENT -> commentRepository.findById(command.targetId())
                    .filter(c -> !c.isRemoved()).orElseThrow(CommentNotFoundException::new).getAuthorId();
        };
        if (ownerId.equals(reporterId))
            throw new ForbiddenOperationException("No puedes denunciar tu propio contenido");
        if (flagRepository.existsByTargetTypeAndTargetIdAndReporterId(command.targetType(), command.targetId(), reporterId))
            throw new DuplicateAbuseFlagException();
        return flagRepository.save(new AbuseFlag(command.targetType(), command.targetId(), reporterId,
                command.reason(), command.details()));
    }

    @Override
    public AbuseFlag handle(ResolveAbuseFlagCommand command) {
        if (!command.admin())
            throw new ForbiddenOperationException("Solo un administrador puede resolver denuncias");
        var flag = flagRepository.findById(command.flagId()).orElseThrow(AbuseFlagNotFoundException::new);
        var moderatorId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var now = Instant.now();

        flag.resolve(command.resolution(), moderatorId, command.note(), now); // valida OPEN y la resolución
        var action = command.resolution() == FlagStatus.ACTION_TAKEN
                ? takeAction(flag, moderatorId, command.note(), now)
                : ModerationActionType.FLAG_DISMISSED;
        moderationActionRepository.save(new ModerationAction(moderatorId, action, flag.getTargetType(),
                flag.getTargetId(), command.note()));

        // Las demás denuncias abiertas sobre el mismo contenido se cierran con la misma resolución.
        flagRepository.findByTargetTypeAndTargetIdAndStatus(flag.getTargetType(), flag.getTargetId(), FlagStatus.OPEN)
                .forEach(other -> {
                    other.resolve(command.resolution(), moderatorId, command.note(), now);
                    flagRepository.save(other);
                });
        return flagRepository.save(flag);
    }

    /** Aplica la sanción sobre el contenido denunciado y dice qué acción quedó en el historial. */
    private ModerationActionType takeAction(AbuseFlag flag, Long moderatorId, String note, Instant now) {
        if (flag.getTargetType() == FlagTargetType.COMMENT) {
            commentRepository.findById(flag.getTargetId()).ifPresent(comment -> {
                comment.remove(now);
                commentRepository.save(comment);
            });
            return ModerationActionType.COMMENT_REMOVED;
        }
        externalReportService.discardReport(flag.getTargetId());
        return ModerationActionType.REPORT_DISCARDED;
    }
}
