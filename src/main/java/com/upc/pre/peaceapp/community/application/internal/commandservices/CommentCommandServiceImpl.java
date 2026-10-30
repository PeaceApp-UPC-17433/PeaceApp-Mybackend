package com.upc.pre.peaceapp.community.application.internal.commandservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.exceptions.CommentNotFoundException;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.commands.AddCommentCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.DeleteCommentCommand;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.ModerationActionType;
import com.upc.pre.peaceapp.community.domain.services.CommentCommandService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ModerationActionRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import com.upc.pre.peaceapp.shared.domain.exceptions.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class CommentCommandServiceImpl implements CommentCommandService {
    private final ReportCommentRepository commentRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final ExternalProfileService externalProfileService;
    private final ExternalReportService externalReportService;

    public CommentCommandServiceImpl(ReportCommentRepository commentRepository,
                                     ModerationActionRepository moderationActionRepository,
                                     ExternalProfileService externalProfileService,
                                     ExternalReportService externalReportService) {
        this.commentRepository = commentRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.externalProfileService = externalProfileService;
        this.externalReportService = externalReportService;
    }

    @Override
    public ReportComment handle(AddCommentCommand command) {
        var authorId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        externalReportService.fetchReport(command.reportId()); // 404 si el reporte no existe
        return commentRepository.save(new ReportComment(command.reportId(), authorId, command.text()));
    }

    @Override
    public void handle(DeleteCommentCommand command) {
        var comment = commentRepository.findById(command.commentId())
                .filter(c -> !c.isRemoved()).orElseThrow(CommentNotFoundException::new);
        var requesterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var isAuthor = comment.isAuthoredBy(requesterId);
        if (!isAuthor && !command.admin())
            throw new ForbiddenOperationException("No tienes permiso para borrar este comentario");
        comment.remove(Instant.now());
        commentRepository.save(comment);
        if (!isAuthor)
            moderationActionRepository.save(new ModerationAction(requesterId, ModerationActionType.COMMENT_REMOVED,
                    FlagTargetType.COMMENT, comment.getId(), "Retirado por un administrador"));
    }
}
