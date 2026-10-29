package com.upc.pre.peaceapp.config;

import com.upc.pre.peaceapp.security.iam.domain.model.valueobjects.Roles;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.SignInResource;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.SignUpResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestAuthHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private final UserIAMRepository userRepository;
    private final RoleRepository roleRepository;

    public TestAuthHelper(MockMvc mockMvc, ObjectMapper objectMapper, UserIAMRepository userRepository,
                          RoleRepository roleRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /** No hay endpoint para crear admins: se registra un usuario normal y se le agrega el rol antes de iniciar sesión. */
    public String registerAndAuthenticateTestAdmin(String username, String password) throws Exception {
        registerAndAuthenticateTestUser(username, password);
        var user = userRepository.findByUsername(username).orElseThrow();
        user.addRole(roleRepository.findByName(Roles.ROLE_ADMIN).orElseThrow());
        userRepository.save(user);
        return registerAndAuthenticateTestUser(username, password);
    }

    public String registerAndAuthenticateTestUser(String username, String password) throws Exception {
        // 🔹 Sign-up
        SignUpResource signUp = new SignUpResource(username, password, "Test", "User", "999888777");
        // Los contextos de test comparten la base H2: si otro test ya registró el correo (409) se reutiliza.
        int status = mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andReturn().getResponse().getStatus();
        if (status != 201 && status != 409) throw new AssertionError("Sign-up falló con estado " + status);

        // 🔹 Sign-in
        SignInResource signIn = new SignInResource(username, password);
        String response = mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signIn)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode tokenNode = objectMapper.readTree(response);
        return tokenNode.get("token").asText();
    }
}