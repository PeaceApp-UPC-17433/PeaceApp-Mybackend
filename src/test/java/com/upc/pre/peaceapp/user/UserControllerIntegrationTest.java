package com.upc.pre.peaceapp.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import com.upc.pre.peaceapp.repositories.UserRepository;
import com.upc.pre.peaceapp.schemas.UpdateUserProfileSchema;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.SignInResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository profileRepository;
    @Autowired private UserIAMRepository iamRepository;
    @Autowired private TestAuthHelper authHelper;

    private Long profileId(String email) {
        return profileRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void whenGetOwnProfile_thenReturnsIt() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("jane@example.com", "password123");

        mockMvc.perform(get("/api/v1/users/jane@example.com").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    void whenGetUnknownProfile_thenNotFoundJson() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("known@example.com", "password123");

        mockMvc.perform(get("/api/v1/users/ghost@example.com").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Perfil no encontrado"));
    }

    @Test
    void whenGetAnotherUsersProfile_thenForbidden() throws Exception {
        authHelper.registerAndAuthenticateTestUser("victim@example.com", "password123");
        String token = authHelper.registerAndAuthenticateTestUser("attacker@example.com", "password123");

        mockMvc.perform(get("/api/v1/users/victim@example.com").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenUpdateOwnProfile_thenFieldsChange() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("tom@example.com", "password123");
        var update = new UpdateUserProfileSchema("Tommy", "White", "123456789", null, null);

        mockMvc.perform(put("/api/v1/users/" + profileId("tom@example.com"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tommy"))
                .andExpect(jsonPath("$.lastname").value("White"));
    }

    @Test
    void whenUpdateAnotherUsersProfile_thenForbidden() throws Exception {
        authHelper.registerAndAuthenticateTestUser("owner@example.com", "password123");
        String token = authHelper.registerAndAuthenticateTestUser("intruder@example.com", "password123");

        mockMvc.perform(put("/api/v1/users/" + profileId("owner@example.com"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileSchema("Hack", null, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenUpdateWithInvalidFields_thenFieldErrors() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("valid@example.com", "password123");

        mockMvc.perform(put("/api/v1/users/" + profileId("valid@example.com"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileSchema(" ", null, "abc", "no-correo", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.phonenumber").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void whenUpdateEmail_thenProfileAndLoginFollow() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("old@example.com", "password123");

        mockMvc.perform(put("/api/v1/users/" + profileId("old@example.com"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileSchema(null, null, null, "New@Example.com", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        assertThat(iamRepository.findByUsername("new@example.com")).isPresent();
        assertThat(iamRepository.findByUsername("old@example.com")).isEmpty();
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignInResource("new@example.com", "password123"))))
                .andExpect(status().isOk());
    }

    @Test
    void whenUpdateEmailToExistingOne_thenConflict() throws Exception {
        authHelper.registerAndAuthenticateTestUser("taken@example.com", "password123");
        String token = authHelper.registerAndAuthenticateTestUser("mover@example.com", "password123");

        mockMvc.perform(put("/api/v1/users/" + profileId("mover@example.com"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserProfileSchema(null, null, null, "taken@example.com", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void whenDeleteOwnAccount_thenProfileAndIdentityAreRemoved() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("del@example.com", "password123");
        Long id = profileId("del@example.com");

        mockMvc.perform(delete("/api/v1/users/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(profileRepository.findById(id)).isEmpty();
        assertThat(iamRepository.findByUsername("del@example.com")).isEmpty();
    }

    @Test
    void whenDeleteAnotherUsersAccount_thenForbiddenAndNothingIsRemoved() throws Exception {
        authHelper.registerAndAuthenticateTestUser("safe@example.com", "password123");
        String token = authHelper.registerAndAuthenticateTestUser("evil@example.com", "password123");

        mockMvc.perform(delete("/api/v1/users/" + profileId("safe@example.com")).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(iamRepository.findByUsername("safe@example.com")).isPresent();
    }
}
