package com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.community.domain.model.aggregates.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
}
