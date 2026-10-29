package com.upc.pre.peaceapp.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LocationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;

    private long createReport(String token, double lat, double lng) throws Exception {
        var body = Map.of("title", "Reporte", "type", "THEFT", "latitude", lat, "longitude", lng, "address", "Calle 1");
        var response = mockMvc.perform(post("/api/v1/reports/").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void whenSaveLocationForExistingReport_thenReportIsMoved() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("locowner@example.com", "password123");
        long id = createReport(token, -12.0, -77.0);

        mockMvc.perform(post("/api/v1/locations/").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-13.5,\"longitude\":-71.9,\"idReport\":" + id + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReport").value(id))
                .andExpect(jsonPath("$.latitude").value(-13.5));

        mockMvc.perform(get("/api/v1/reports/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.longitude").value(-71.9));
    }

    @Test
    void whenSaveLocationForUnknownReport_thenNotFound() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("locunknown@example.com", "password123");

        mockMvc.perform(post("/api/v1/locations/").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-13.5,\"longitude\":-71.9,\"idReport\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenSaveLocationWithInvalidCoordinates_thenBadRequestPerField() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("locinvalid@example.com", "password123");

        mockMvc.perform(post("/api/v1/locations/").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":200,\"idReport\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());
    }

    @Test
    void whenSaveLocationOfAnotherUsersReport_thenForbidden() throws Exception {
        String owner = authHelper.registerAndAuthenticateTestUser("locowner2@example.com", "password123");
        String other = authHelper.registerAndAuthenticateTestUser("locother@example.com", "password123");
        long id = createReport(owner, -12.0, -77.0);

        mockMvc.perform(post("/api/v1/locations/").header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":1,\"longitude\":1,\"idReport\":" + id + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetLocations_thenReturnsReportCoordinates() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("loclist@example.com", "password123");
        createReport(token, -12.0, -77.0);

        mockMvc.perform(get("/api/v1/locations/").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].idReport").isNumber());
    }

    @Test
    void whenGetDangerousLocations_thenOnlyZonesWithEnoughReports() throws Exception {
        String token = authHelper.registerAndAuthenticateTestUser("locdanger@example.com", "password123");
        // Zona aislada: dos reportes en la misma celda
        createReport(token, -70.0011, -150.0011);
        createReport(token, -70.0012, -150.0012);

        mockMvc.perform(get("/api/v1/locations/dangerous?quantity_reports=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.latitude < -69.9 && @.latitude > -70.1)]", hasSize(1)));

        mockMvc.perform(get("/api/v1/locations/dangerous").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
