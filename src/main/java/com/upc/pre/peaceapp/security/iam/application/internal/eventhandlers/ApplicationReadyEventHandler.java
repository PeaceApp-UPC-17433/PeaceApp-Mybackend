package com.upc.pre.peaceapp.security.iam.application.internal.eventhandlers;


import com.upc.pre.peaceapp.security.iam.domain.model.commands.SeedAdminCommand;
import com.upc.pre.peaceapp.security.iam.domain.model.commands.SeedRolesCommand;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMCommandService;
import com.upc.pre.peaceapp.security.iam.domain.services.RoleCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class ApplicationReadyEventHandler {
    private final RoleCommandService roleCommandService;
    private final UserIAMCommandService userCommandService;
    private final String adminEmail;
    private final String adminPassword;
    private static final int MIN_ADMIN_PASSWORD = 8;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReadyEventHandler.class);


    public ApplicationReadyEventHandler(RoleCommandService roleCommandService,
                                        UserIAMCommandService userCommandService,
                                        @Value("${peaceapp.admin.email:}") String adminEmail,
                                        @Value("${peaceapp.admin.password:}") String adminPassword) {
        this.roleCommandService = roleCommandService;
        this.userCommandService = userCommandService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        var applicationName = event.getApplicationContext().getId();
        LOGGER.info("Starting to verify if roles seeding is needed for {} at {}", applicationName, currentTimestamp());
        var seedRolesCommand = new SeedRolesCommand();
        roleCommandService.handle(seedRolesCommand);
        LOGGER.info("Roles seeding verification finished for {} at {}", applicationName, currentTimestamp());
        seedAdmin();
    }

    /** Solo si se configuran correo y contraseña. Un fallo se registra pero no impide arrancar. */
    private void seedAdmin() {
        if (adminEmail.isBlank() && adminPassword.isBlank()) return;
        if (adminEmail.isBlank() || adminPassword.length() < MIN_ADMIN_PASSWORD) {
            LOGGER.warn("Admin seeding omitido: peaceapp.admin.email es obligatorio y peaceapp.admin.password necesita al menos {} caracteres", MIN_ADMIN_PASSWORD);
            return;
        }
        try {
            var created = userCommandService.handle(new SeedAdminCommand(adminEmail, adminPassword, "Administrador", "PeaceApp"));
            LOGGER.info(created ? "Cuenta de administrador creada" : "La cuenta de administrador ya existía");
        } catch (RuntimeException ex) {
            LOGGER.error("No se pudo crear la cuenta de administrador", ex);
        }
    }

    private Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}