package com.upc.pre.peaceapp.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"peaceapp.admin.email=seed-admin@example.com", "peaceapp.admin.password=password123"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSeedIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void configuredAdminCanSignInAndUseTheAdminPanel() throws Exception {
        var response = mockMvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "seed-admin@example.com", "password", "password123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var token = objectMapper.readTree(response).get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/v1/admin/metrics").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        // Y tiene perfil, como cualquier cuenta creada por el registro.
        mockMvc.perform(get("/api/v1/users/seed-admin@example.com").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
