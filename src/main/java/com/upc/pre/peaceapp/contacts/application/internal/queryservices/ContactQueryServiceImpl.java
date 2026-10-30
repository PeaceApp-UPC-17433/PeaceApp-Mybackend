package com.upc.pre.peaceapp.contacts.application.internal.queryservices;

import com.upc.pre.peaceapp.contacts.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.contacts.domain.model.aggregates.TrustedContact;
import com.upc.pre.peaceapp.contacts.domain.model.queries.*;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactView;
import com.upc.pre.peaceapp.contacts.domain.services.ContactQueryService;
import com.upc.pre.peaceapp.contacts.infrastructure.persistence.jpa.repositories.TrustedContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContactQueryServiceImpl implements ContactQueryService {
    private final TrustedContactRepository repository;
    private final ExternalProfileService externalProfileService;

    public ContactQueryServiceImpl(TrustedContactRepository repository, ExternalProfileService externalProfileService) {
        this.repository = repository;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public List<ContactView> handle(GetContactsQuery query) {
        var me = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return views(repository.findByParticipantAndStatus(me, ContactStatus.ACCEPTED), me);
    }

    @Override
    public List<ContactView> handle(GetIncomingRequestsQuery query) {
        var me = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return views(repository.findByAddresseeIdAndStatusOrderByIdDesc(me, ContactStatus.PENDING), me);
    }

    @Override
    public List<ContactView> handle(GetOutgoingRequestsQuery query) {
        var me = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return views(repository.findByRequesterIdAndStatusOrderByIdDesc(me, ContactStatus.PENDING), me);
    }

    @Override
    public List<ContactView> handle(GetBlockedContactsQuery query) {
        var me = externalProfileService.fetchProfileIdByEmail(query.requesterEmail());
        return views(repository.findByBlockedByAndStatusOrderByIdDesc(me, ContactStatus.BLOCKED), me);
    }

    @Override
    public List<Long> handle(GetAcceptedContactProfileIdsQuery query) {
        return repository.findByParticipantAndStatus(query.profileId(), ContactStatus.ACCEPTED).stream()
                .map(c -> c.otherParty(query.profileId())).toList();
    }

    private List<ContactView> views(List<TrustedContact> contacts, Long me) {
        return contacts.stream().map(c -> {
            var other = externalProfileService.fetchProfile(c.otherParty(me));
            return new ContactView(c.getId(), other.id(), other.name(), other.lastname(), other.email(), c.getStatus(),
                    c.getRequesterId().equals(me), c.getCreatedAt().toInstant(), c.getRespondedAt());
        }).toList();
    }
}
