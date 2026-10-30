package com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    @Query("select r.status, count(r) from Report r group by r.status")
    List<Object[]> countGroupedByStatus();

    /** Fechas de los reportes ya atendidos, los más recientes primero; el tope evita cargar toda la tabla. */
    @Query("select r.createdAt, r.updatedAt from Report r where r.status in :statuses order by r.updatedAt desc")
    List<Object[]> findAttentionDates(@Param("statuses") Collection<ReportStatus> statuses, Pageable pageable);
}
