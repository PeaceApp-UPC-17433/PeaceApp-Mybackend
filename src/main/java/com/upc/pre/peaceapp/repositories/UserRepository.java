package com.upc.pre.peaceapp.repositories;

import com.upc.pre.peaceapp.models.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
}
