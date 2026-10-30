package com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportVote;
import com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportVoteRepository extends JpaRepository<ReportVote, Long> {
    Optional<ReportVote> findByReportIdAndVoterId(Long reportId, Long voterId);

    long countByReportIdAndType(Long reportId, VoteType type);

    long countByType(VoteType type);

    void deleteByReportId(Long reportId);
}
