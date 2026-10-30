package com.upc.pre.peaceapp.contacts.infrastructure.persistence.jpa.repositories;

import com.upc.pre.peaceapp.contacts.domain.model.aggregates.TrustedContact;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {
    Optional<TrustedContact> findByPairKey(String pairKey);

    @Query("select c from TrustedContact c where (c.requesterId = :id or c.addresseeId = :id) and c.status = :status order by c.id desc")
    List<TrustedContact> findByParticipantAndStatus(@Param("id") Long profileId, @Param("status") ContactStatus status);

    List<TrustedContact> findByAddresseeIdAndStatusOrderByIdDesc(Long addresseeId, ContactStatus status);

    List<TrustedContact> findByRequesterIdAndStatusOrderByIdDesc(Long requesterId, ContactStatus status);

    List<TrustedContact> findByBlockedByAndStatusOrderByIdDesc(Long blockedBy, ContactStatus status);

    @Modifying
    @Query("delete from TrustedContact c where c.requesterId = :id or c.addresseeId = :id")
    void deleteByParticipant(@Param("id") Long profileId);
}
