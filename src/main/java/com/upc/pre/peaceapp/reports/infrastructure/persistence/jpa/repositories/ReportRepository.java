package com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
}
