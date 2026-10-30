package com.upc.pre.peaceapp.security.iam.interfaces.rest;

import com.upc.pre.peaceapp.security.iam.domain.model.commands.ChangeUserRoleCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.CreateAdminCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.queries.GetAllUsersQuery;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMCommandService;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMQueryService;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.ChangeUserRoleResource;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.CreateAdminResource;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.UserIAMResource;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de cuentas y roles. Solo un administrador autenticado puede usarla. */
@RestController
@RequestMapping(value = "/api/v1/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin Users", description = "Administración de usuarios y roles")
public class AdminUserIAMController {
    private final UserIAMCommandService commandService;
    private final UserIAMQueryService queryService;

    public AdminUserIAMController(UserIAMCommandService commandService, UserIAMQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<List<UserIAMResource>> getAll() {
        var users = queryService.handle(new GetAllUsersQuery()).stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserIAMResource> createAdmin(@Valid @RequestBody CreateAdminResource resource) {
        var user = commandService.handle(new CreateAdminCommand(
                resource.email(), resource.password(), resource.name(), resource.lastname()));
        return new ResponseEntity<>(UserResourceFromEntityAssembler.toResourceFromEntity(user), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserIAMResource> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeUserRoleResource resource) {
        var user = commandService.handle(new ChangeUserRoleCommand(id, resource.role()));
        return ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user));
    }
}
