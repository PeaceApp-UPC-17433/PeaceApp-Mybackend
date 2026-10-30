package com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    Page<ReportComment> findByReportIdAndRemovedFalse(Long reportId, Pageable pageable);

    long countByRemovedFalse();

    @Query("select c.id from ReportComment c where c.reportId = :reportId")
    List<Long> findIdsByReportId(@Param("reportId") Long reportId);

    void deleteByReportId(Long reportId);
}
