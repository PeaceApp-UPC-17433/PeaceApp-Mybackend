package com.upc.pre.peaceapp.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import com.upc.pre.peaceapp.models.Alert;
import com.upc.pre.peaceapp.schemas.AlertSchema;
import com.upc.pre.peaceapp.services.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AlertControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;
    @Autowired private AlertService alertService;

    @Test
    void whenCreateAlert_thenReturnsCreated() throws Exception {
        // 🔹 Arrange
        String token = authHelper.registerAndAuthenticateTestUser("alertuser@example.com", "password123");

        AlertSchema alert = new AlertSchema(
                "Miraflores",
                "Accident",
                "Crash at intersection",
                1,
                null,
                null
        );

        // 🔹 Act & Assert
        mockMvc.perform(post("/api/v1/alerts/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alert)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location").value("Miraflores"))
                .andExpect(jsonPath("$.type").value("Accident"));
    }

    @Test
    void whenGetAlertById_thenReturnsAlert() throws Exception {
        // 🔹 Arrange
        Alert alert = alertService.saveAlert(new AlertSchema("San Isidro", "Robbery", "Reported robbery", 2, null, null));
        String token = authHelper.registerAndAuthenticateTestUser("viewalert@example.com", "password123");

        // 🔹 Act & Assert
        mockMvc.perform(get("/api/v1/alerts/" + alert.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Robbery"));
    }

    @Test
    void whenGetAlertsByUserId_thenReturnsList() throws Exception {
        // 🔹 Arrange
        alertService.saveAlert(new AlertSchema("Lima", "Fire", "Burning building", 99, null, null));
        alertService.saveAlert(new AlertSchema("Lima", "Flood", "Overflow", 99, null, null));

        String token = authHelper.registerAndAuthenticateTestUser("user99@example.com", "password123");

        // 🔹 Act & Assert
        mockMvc.perform(get("/api/v1/alerts/user/99")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void whenGetAllAlerts_thenReturnsListOrNoContent() throws Exception {
        // 🔹 Arrange
        alertService.saveAlert(new AlertSchema("Callao", "Accident", "Dock collision", 77, null, null));

        String token = authHelper.registerAndAuthenticateTestUser("reader@example.com", "password123");

        // 🔹 Act & Assert
        mockMvc.perform(get("/api/v1/alerts/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void whenDeleteAllAlerts_thenAllAreRemoved() throws Exception {
        // 🔹 Arrange
        alertService.saveAlert(new AlertSchema("Breña", "Warning", "Bridge crack", 15, null, null));
        String token = authHelper.registerAndAuthenticateTestUser("deleter@example.com", "password123");

        // 🔹 Act
        mockMvc.perform(delete("/api/v1/alerts/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All alerts have been deleted"));

        // 🔹 Assert
        List<Alert> remaining = alertService.findAll();
        assertThat(remaining).isEmpty();
    }
}
