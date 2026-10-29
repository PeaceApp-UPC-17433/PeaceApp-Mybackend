package com.upc.pre.peaceapp.reports.application.internal.queryservices;

import com.upc.pre.peaceapp.reports.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.reports.domain.exceptions.ReportNotFoundException;
import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetHeatmapQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportByIdQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.GetReportsInAreaQuery;
import com.upc.pre.peaceapp.reports.domain.model.queries.SearchReportsQuery;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.HeatCell;
import com.upc.pre.peaceapp.reports.domain.services.ReportQueryService;
import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories.ReportHeatmapRepository;
import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories.ReportRepository;
import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories.ReportSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportQueryServiceImpl implements ReportQueryService {
    private final ReportRepository reportRepository;
    private final ReportHeatmapRepository heatmapRepository;
    private final ExternalProfileService externalProfileService;

    public ReportQueryServiceImpl(ReportRepository reportRepository, ReportHeatmapRepository heatmapRepository,
                                  ExternalProfileService externalProfileService) {
        this.reportRepository = reportRepository;
        this.heatmapRepository = heatmapRepository;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public Report handle(GetReportByIdQuery query) {
        return reportRepository.findById(query.reportId()).orElseThrow(ReportNotFoundException::new);
    }

    @Override
    public Page<Report> handle(SearchReportsQuery query) {
        var reporterId = query.mineEmail() != null
                ? externalProfileService.fetchProfileIdByEmail(query.mineEmail())
                : query.reporterId();
        var spec = ReportSpecifications.matching(query.filter(), query.text(), reporterId, null);
        return reportRepository.findAll(spec, query.pageable());
    }

    @Override
    public List<Report> handle(GetReportsInAreaQuery query) {
        var spec = ReportSpecifications.matching(query.filter(), null, null, query.area().box());
        var newestFirst = PageRequest.of(0, query.limit(), Sort.by(Sort.Direction.DESC, "occurredAt"));
        // La caja contiene al círculo; si el área es un círculo, se descartan las esquinas
        return reportRepository.findAll(spec, newestFirst).stream()
                .filter(report -> query.area().contains(report.getLocation()))
                .toList();
    }

    @Override
    public List<HeatCell> handle(GetHeatmapQuery query) {
        return heatmapRepository.aggregate(query.box(), query.cellSizeDegrees(), query.minCount());
    }
}
