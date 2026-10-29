package com.upc.pre.peaceapp.notifications.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.notifications.domain.model.aggregates.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {
    List<DeviceRegistration> findByProfileId(Long profileId);

    Optional<DeviceRegistration> findByToken(String token);
}
