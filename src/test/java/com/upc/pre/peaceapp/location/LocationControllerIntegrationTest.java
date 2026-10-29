package com.upc.pre.peaceapp.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import com.upc.pre.peaceapp.schemas.LocationSchema;
import com.upc.pre.peaceapp.services.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LocationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;
    @Autowired private LocationService locationService;

    @Test
    void whenCreateLocation_thenReturnsCreated() throws Exception {
        // 🔹 Arrange
        String token = authHelper.registerAndAuthenticateTestUser("locationuser@example.com", "password123");

        LocationSchema location = new LocationSchema(
                "-12.0464", "-77.0428", 1L
        );

        // 🔹 Act & Assert
        mockMvc.perform(post("/api/v1/locations/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alatitude").value("-12.0464"))
                .andExpect(jsonPath("$.alongitude").value("-77.0428"));
    }

    @Test
    void whenGetAllLocations_thenReturnsList() throws Exception {
        // 🔹 Arrange
        String token = authHelper.registerAndAuthenticateTestUser("viewer@example.com", "password123");

        // 🔹 Act & Assert
        mockMvc.perform(get("/api/v1/locations/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void whenGetDangerousLocations_thenReturnsFilteredList() throws Exception {
        // 🔹 Arrange
        String token = authHelper.registerAndAuthenticateTestUser("filter@example.com", "password123");

        int quantityReports = 1;

        // 🔹 Act & Assert
        mockMvc.perform(get("/api/v1/locations/dangerous")
                        .param("quantity_reports", String.valueOf(quantityReports))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
