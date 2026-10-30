package com.upc.pre.peaceapp.contacts.interfaces.rest;

import com.upc.pre.peaceapp.contacts.domain.model.commands.*;
import com.upc.pre.peaceapp.contacts.domain.model.queries.*;
import com.upc.pre.peaceapp.contacts.domain.model.valueobjects.ContactView;
import com.upc.pre.peaceapp.contacts.domain.services.ContactCommandService;
import com.upc.pre.peaceapp.contacts.domain.services.ContactQueryService;
import com.upc.pre.peaceapp.contacts.interfaces.rest.resources.ContactResource;
import com.upc.pre.peaceapp.contacts.interfaces.rest.resources.RequestContactResource;
import com.upc.pre.peaceapp.contacts.interfaces.rest.transform.ContactResourceFromViewAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Contactos de confianza del usuario autenticado. Todos los endpoints piden token; quien actúa sale del token.
 * Los errores los traduce {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(value = "/api/v1/contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Contacts", description = "Trusted contacts")
public class ContactsController {
    private final ContactCommandService commandService;
    private final ContactQueryService queryService;

    public ContactsController(ContactCommandService commandService, ContactQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<List<ContactResource>> getContacts(Authentication authentication) {
        return ResponseEntity.ok(toResources(queryService.handle(new GetContactsQuery(authentication.getName()))));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<ContactResource>> getIncoming(Authentication authentication) {
        return ResponseEntity.ok(toResources(queryService.handle(new GetIncomingRequestsQuery(authentication.getName()))));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<ContactResource>> getOutgoing(Authentication authentication) {
        return ResponseEntity.ok(toResources(queryService.handle(new GetOutgoingRequestsQuery(authentication.getName()))));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<ContactResource>> getBlocked(Authentication authentication) {
        return ResponseEntity.ok(toResources(queryService.handle(new GetBlockedContactsQuery(authentication.getName()))));
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> request(@Valid @RequestBody RequestContactResource resource,
                                        Authentication authentication) {
        commandService.handle(new RequestContactCommand(authentication.getName(), resource.email()));
        return ResponseEntity.created(URI.create("/api/v1/contacts/requests/outgoing")).build();
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new AcceptContactCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new DeclineContactCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<Void> block(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new BlockContactCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication authentication) {
        commandService.handle(new RemoveContactCommand(id, authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    private static List<ContactResource> toResources(List<ContactView> views) {
        return views.stream().map(ContactResourceFromViewAssembler::toResource).toList();
    }
}
