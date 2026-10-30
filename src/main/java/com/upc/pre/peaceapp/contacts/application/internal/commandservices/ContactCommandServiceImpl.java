package com.upc.pre.peaceapp.contacts.application.internal.commandservices;

import com.upc.pre.peaceapp.contacts.application.internal.outboundservices.acl.ExternalProfileService;
import com.upc.pre.peaceapp.contacts.domain.exceptions.ContactConflictException;
import com.upc.pre.peaceapp.contacts.domain.exceptions.ContactNotFoundException;
import com.upc.pre.peaceapp.contacts.domain.model.aggregates.TrustedContact;
import com.upc.pre.peaceapp.contacts.domain.model.commands.*;
import com.upc.pre.peaceapp.contacts.domain.model.events.ContactAcceptedEvent;
import com.upc.pre.peaceapp.contacts.domain.model.events.ContactRequestedEvent;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactStatus;
import com.upc.pre.peaceapp.contacts.domain.services.ContactCommandService;
import com.upc.pre.peaceapp.contacts.infrastructure.persistence.jpa.repositories.TrustedContactRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ContactCommandServiceImpl implements ContactCommandService {
    private final TrustedContactRepository repository;
    private final ExternalProfileService externalProfileService;
    private final ApplicationEventPublisher eventPublisher;

    public ContactCommandServiceImpl(TrustedContactRepository repository, ExternalProfileService externalProfileService,
                                     ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.externalProfileService = externalProfileService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public TrustedContact handle(RequestContactCommand command) {
        var requesterId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var targetId = externalProfileService.fetchProfileIdByEmail(command.targetEmail());
        var contact = new TrustedContact(requesterId, targetId); // rechaza pedirse a uno mismo

        repository.findByPairKey(contact.getPairKey()).ifPresent(existing -> {
            throw new ContactConflictException(conflictMessage(existing, requesterId));
        });
        var saved = repository.save(contact);
        var requester = externalProfileService.fetchProfile(requesterId);
        eventPublisher.publishEvent(new ContactRequestedEvent(saved.getId(), requesterId, targetId, requester.name()));
        return saved;
    }

    @Override
    public TrustedContact handle(AcceptContactCommand command) {
        var profileId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var contact = findInvolving(command.contactId(), profileId);
        contact.accept(profileId, Instant.now());
        var saved = repository.save(contact);
        var addressee = externalProfileService.fetchProfile(profileId);
        eventPublisher.publishEvent(new ContactAcceptedEvent(saved.getId(), saved.getRequesterId(),
                saved.getAddresseeId(), addressee.name()));
        return saved;
    }

    @Override
    public void handle(DeclineContactCommand command) {
        var profileId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var contact = findInvolving(command.contactId(), profileId);
        contact.assertCanDecline(profileId);
        repository.delete(contact);
    }

    @Override
    public TrustedContact handle(BlockContactCommand command) {
        var profileId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var contact = findInvolving(command.contactId(), profileId);
        contact.block(profileId, Instant.now());
        return repository.save(contact);
    }

    @Override
    public void handle(RemoveContactCommand command) {
        var profileId = externalProfileService.fetchProfileIdByEmail(command.requesterEmail());
        var contact = findInvolving(command.contactId(), profileId);
        contact.assertCanRemove(profileId);
        repository.delete(contact);
    }

    @Override
    public void purgeProfile(Long profileId) {
        repository.deleteByParticipant(profileId);
    }

    /** Quien no participa recibe 404, igual que si la relación no existiera: no se revela su existencia. */
    private TrustedContact findInvolving(Long contactId, Long profileId) {
        return repository.findById(contactId).filter(c -> c.involves(profileId))
                .orElseThrow(ContactNotFoundException::new);
    }

    private String conflictMessage(TrustedContact existing, Long requesterId) {
        if (existing.getStatus() == ContactStatus.ACCEPTED) return "Ya son contactos de confianza";
        if (existing.getStatus() == ContactStatus.PENDING)
            return existing.getRequesterId().equals(requesterId)
                    ? "Ya enviaste una solicitud a este usuario"
                    : "Este usuario ya te envió una solicitud: acéptala";
        return "No se puede enviar la solicitud a este usuario";
    }
}
