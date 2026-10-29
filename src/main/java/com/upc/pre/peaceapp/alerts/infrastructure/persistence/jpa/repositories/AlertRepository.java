package com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.Alert;
import com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Alert> findByRecipientIdAndStatus(Long recipientId, AlertStatus status, Pageable pageable);

    long countByRecipientIdAndStatus(Long recipientId, AlertStatus status);

    boolean existsByRecipientIdAndReportId(Long recipientId, Long reportId);

    @Modifying(clearAutomatically = true)
    @Query("update Alert a set a.status = com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus.READ, a.readAt = :now "
            + "where a.recipientId = :recipientId and a.status = com.upc.pre.peaceapp.alerts.domain.model.valueobjects.AlertStatus.UNREAD")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("delete from Alert a where a.recipientId = :recipientId")
    int deleteAllByRecipient(@Param("recipientId") Long recipientId);
}
