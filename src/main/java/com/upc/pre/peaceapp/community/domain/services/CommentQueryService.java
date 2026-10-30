package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.queries.GetCommentsByReportQuery;
import org.springframework.data.domain.Page;

public interface CommentQueryService {
    /** Comentarios visibles (no retirados) de un reporte; 404 si el reporte no existe. */
    Page<ReportComment> handle(GetCommentsByReportQuery query);
}
