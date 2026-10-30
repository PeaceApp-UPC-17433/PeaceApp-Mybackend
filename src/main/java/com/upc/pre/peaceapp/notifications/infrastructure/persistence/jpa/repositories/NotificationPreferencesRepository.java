package com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.notifications.domain.model.aggregates.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
    Optional<NotificationPreferences> findByProfileId(Long profileId);
}
