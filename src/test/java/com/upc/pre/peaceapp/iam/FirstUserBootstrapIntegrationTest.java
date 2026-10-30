package com.upc.pre.peaceapp.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.security.iam.interfaces.rest.resources.SignUpResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto propio (BD H2 propia) para que la tabla de usuarios esté realmente vacía al primer registro. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"peaceapp.bootstrap.first-user-admin=true",
                "spring.datasource.url=jdbc:h2:mem:bootstrapdb;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class FirstUserBootstrapIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private org.springframework.test.web.servlet.ResultActions signUp(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignUpResource(email, "secure123", "Ana", "Pérez", "999888777"))));
    }

    @Test
    void firstSignUpIsAdminAndLaterOnesAreRegularUsers() throws Exception {
        signUp("first@example.com").andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")));
        signUp("second@example.com").andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", contains("ROLE_USER")));
    }
}
