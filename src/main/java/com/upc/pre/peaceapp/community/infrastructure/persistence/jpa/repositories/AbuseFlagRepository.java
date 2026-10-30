package com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.community.domain.model.aggregates.AbuseFlag;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagStatus;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.FlagTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AbuseFlagRepository extends JpaRepository<AbuseFlag, Long> {
    Page<AbuseFlag> findByStatus(FlagStatus status, Pageable pageable);

    boolean existsByTargetTypeAndTargetIdAndReporterId(FlagTargetType targetType, Long targetId, Long reporterId);

    List<AbuseFlag> findByTargetTypeAndTargetIdAndStatus(FlagTargetType targetType, Long targetId, FlagStatus status);

    long countByStatus(FlagStatus status);

    void deleteByTargetTypeAndTargetId(FlagTargetType targetType, Long targetId);

    void deleteByTargetTypeAndTargetIdIn(FlagTargetType targetType, Collection<Long> targetIds);
}
