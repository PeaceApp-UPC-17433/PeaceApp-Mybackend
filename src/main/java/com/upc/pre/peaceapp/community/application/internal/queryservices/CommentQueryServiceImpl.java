package com.upc.pre.peaceapp.community.application.internal.queryservices;

import com.upc.pre.peaceapp.community.application.internal.outboundservices.acl.ExternalReportService;
import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommentsByReportQuery;
import com.upc.pre.peaceapp.community.domain.services.CommentQueryService;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentQueryServiceImpl implements CommentQueryService {
    private final ReportCommentRepository commentRepository;
    private final ExternalReportService externalReportService;

    public CommentQueryServiceImpl(ReportCommentRepository commentRepository,
                                   ExternalReportService externalReportService) {
        this.commentRepository = commentRepository;
        this.externalReportService = externalReportService;
    }

    @Override
    public Page<ReportComment> handle(GetCommentsByReportQuery query) {
        externalReportService.fetchReport(query.reportId()); // 404 si el reporte no existe
        return commentRepository.findByReportIdAndRemovedFalse(query.reportId(), query.pageable());
    }
}
