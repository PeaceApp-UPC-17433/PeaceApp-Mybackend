package com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories;
import com.upc.pre.peaceapp.security.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserIAMRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
