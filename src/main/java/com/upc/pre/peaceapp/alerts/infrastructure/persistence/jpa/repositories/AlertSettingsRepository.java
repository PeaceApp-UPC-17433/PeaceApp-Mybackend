package com.upc.pre.peaceapp.alerts.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.alerts.domain.model.aggregates.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Long> {
    Optional<AlertSettings> findByProfileId(Long profileId);

    /** Preselección barata por rectángulo; la distancia exacta y el radio de cada usuario se comprueban después. */
    @Query("select s from AlertSettings s where s.lastLocation.latitude between :minLat and :maxLat "
            + "and s.lastLocation.longitude between :minLng and :maxLng and s.locationUpdatedAt >= :since")
    List<AlertSettings> findNear(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                 @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                 @Param("since") Instant since);
}
