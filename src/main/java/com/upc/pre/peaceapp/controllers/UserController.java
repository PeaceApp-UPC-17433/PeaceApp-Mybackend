package com.upc.pre.peaceapp.controllers;

import com.upc.pre.peaceapp.models.UserProfile;
import com.upc.pre.peaceapp.schemas.UpdateUserProfileSchema;
import com.upc.pre.peaceapp.services.UserService;
import com.upc.pre.peaceapp.shared.interfaces.rest.AuthenticatedRequester;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * La creación de perfiles ocurre en el registro ({@code POST /api/v1/authentication/sign-up}),
 * por eso aquí no hay POST.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Users API")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{email}")
    public ResponseEntity<UserProfile> getUserByEmail(@PathVariable String email, Authentication authentication) {
        return ResponseEntity.ok(userService.findByEmail(email, authentication.getName(), isAdmin(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateUser(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserProfileSchema user,
                                                  Authentication authentication) {
        return ResponseEntity.ok(userService.update(id, user, authentication.getName(), isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.delete(id, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(Authentication authentication) {
        return AuthenticatedRequester.isAdmin(authentication);
    }
}
