package com.upc.pre.peaceapp.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserManagementIntegrationTest {
    private static final String BASE = "/api/v1/admin/users";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;
    @Autowired private UserIAMRepository userRepository;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String token, Object body) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        return mockMvc.perform(request);
    }

    private Map<String, String> newAdmin(String email) {
        return Map.of("email", email, "password", "secure123", "name", "Ada", "lastname", "Admin");
    }

    @Test
    void regularUserAndAnonymousCannotUseAdminUserEndpoints() throws Exception {
        var userToken = authHelper.registerAndAuthenticateTestUser("plain-user@example.com", "secure123");
        send(get(BASE), userToken, null).andExpect(status().isForbidden());
        send(post(BASE), userToken, newAdmin("x1@example.com")).andExpect(status().isForbidden());
        send(get(BASE), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesAnotherAdminWhoCanSignInAndManageUsers() throws Exception {
        var adminToken = authHelper.registerAndAuthenticateTestAdmin("root-admin@example.com", "secure123");
        send(post(BASE), adminToken, newAdmin("second-admin@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("ROLE_ADMIN")));
        send(post(BASE), adminToken, newAdmin("second-admin@example.com")).andExpect(status().isConflict());

        var secondToken = authHelper.registerAndAuthenticateTestUser("second-admin@example.com", "secure123");
        send(get(BASE), secondToken, null).andExpect(status().isOk());
    }

    @Test
    void adminPromotesAndDemotesUsers() throws Exception {
        var adminToken = authHelper.registerAndAuthenticateTestAdmin("promoter@example.com", "secure123");
        authHelper.registerAndAuthenticateTestUser("promotee@example.com", "secure123");
        var id = userRepository.findByUsername("promotee@example.com").orElseThrow().getId();

        send(put(BASE + "/" + id + "/role"), adminToken, Map.of("role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("ROLE_ADMIN")));
        send(put(BASE + "/" + id + "/role"), adminToken, Map.of("role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("ROLE_ADMIN"))));
        send(put(BASE + "/999999/role"), adminToken, Map.of("role", "ROLE_ADMIN")).andExpect(status().isNotFound());
        send(put(BASE + "/" + id + "/role"), adminToken, Map.of("role", "ROLE_SUPER")).andExpect(status().isBadRequest());
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        var adminToken = authHelper.registerAndAuthenticateTestAdmin("only-admin@example.com", "secure123");
        // Se deja a este como único admin quitando el rol a los demás.
        userRepository.findAll().stream()
                .filter(u -> u.hasRole(com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles.ROLE_ADMIN))
                .filter(u -> !u.getUsername().equals("only-admin@example.com"))
                .forEach(u -> {
                    try {
                        send(put(BASE + "/" + u.getId() + "/role"), adminToken, Map.of("role", "ROLE_USER")).andExpect(status().isOk());
                    } catch (Exception e) { throw new AssertionError(e); }
                });
        var id = userRepository.findByUsername("only-admin@example.com").orElseThrow().getId();
        send(put(BASE + "/" + id + "/role"), adminToken, Map.of("role", "ROLE_USER")).andExpect(status().isConflict());
        assertTrue(userRepository.findByUsername("only-admin@example.com").orElseThrow().hasRole(
                com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles.ROLE_ADMIN));
    }
}
