package com.upc.pre.peaceapp.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cada prueba usa su propia zona del mapa y sus propios usuarios, porque los contextos de test
 * comparten la base H2 y los reportes de otras pruebas no deben generar alertas aquí.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private void sendLocation(String token, double lat, double lng) throws Exception {
        mockMvc.perform(json(put("/api/v1/alerts/settings/location"), token, Map.of("latitude", lat, "longitude", lng)))
                .andExpect(status().isOk());
    }

    private long createReport(String token, double lat, double lng, String severity) throws Exception {
        var body = Map.of("title", "Incidente de prueba", "type", "THEFT", "severity", severity,
                "latitude", lat, "longitude", lng, "address", "Av. Prueba 1");
        var response = mockMvc.perform(json(post("/api/v1/reports"), token, body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private JsonNode alertsOf(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(auth(get("/api/v1/alerts"), token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long profileId(String token, String email) throws Exception {
        return objectMapper.readTree(mockMvc.perform(auth(get("/api/v1/users/" + email), token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void newReportAlertsOnlyNearbyUsersAndNotTheReporter() throws Exception {
        var reporter = authHelper.registerAndAuthenticateTestUser("alert-reporter1@example.com", "password123");
        var near = authHelper.registerAndAuthenticateTestUser("alert-near1@example.com", "password123");
        var far = authHelper.registerAndAuthenticateTestUser("alert-far1@example.com", "password123");
        sendLocation(reporter, 40.0000, 10.0000);
        sendLocation(near, 40.0030, 10.0000);   // unos 330 m
        sendLocation(far, 40.2000, 10.0000);    // unos 22 km

        var reportId = createReport(reporter, 40.0, 10.0, "HIGH");

        var alerts = alertsOf(near);
        org.assertj.core.api.Assertions.assertThat(alerts.get("totalElements").asLong()).isEqualTo(1);
        var alert = alerts.get("content").get(0);
        org.assertj.core.api.Assertions.assertThat(alert.get("reportId").asLong()).isEqualTo(reportId);
        org.assertj.core.api.Assertions.assertThat(alert.get("status").asText()).isEqualTo("UNREAD");
        org.assertj.core.api.Assertions.assertThat(alert.get("distanceMeters").asInt()).isBetween(300, 360);
        org.assertj.core.api.Assertions.assertThat(alertsOf(reporter).get("totalElements").asLong()).isZero();
        org.assertj.core.api.Assertions.assertThat(alertsOf(far).get("totalElements").asLong()).isZero();

        mockMvc.perform(auth(get("/api/v1/alerts/unread-count"), near))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void smallRadiusMinSeverityAndMuteSuppressAlerts() throws Exception {
        var reporter = authHelper.registerAndAuthenticateTestUser("alert-reporter2@example.com", "password123");
        var user = authHelper.registerAndAuthenticateTestUser("alert-user2@example.com", "password123");
        sendLocation(user, 41.0030, 11.0000); // unos 330 m del reporte

        mockMvc.perform(json(put("/api/v1/alerts/settings"), user, Map.of("radiusMeters", 200, "minSeverity", "LOW")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.radiusMeters").value(200));
        createReport(reporter, 41.0, 11.0, "HIGH");
        org.assertj.core.api.Assertions.assertThat(alertsOf(user).get("totalElements").asLong()).isZero();

        mockMvc.perform(json(put("/api/v1/alerts/settings"), user, Map.of("radiusMeters", 1000, "minSeverity", "CRITICAL")))
                .andExpect(status().isOk());
        createReport(reporter, 41.0, 11.0, "HIGH");
        org.assertj.core.api.Assertions.assertThat(alertsOf(user).get("totalElements").asLong()).isZero();

        mockMvc.perform(json(put("/api/v1/alerts/settings"), user,
                        Map.of("radiusMeters", 1000, "minSeverity", "LOW", "mutedUntil", "2999-01-01T00:00:00Z")))
                .andExpect(status().isOk());
        createReport(reporter, 41.0, 11.0, "CRITICAL");
        org.assertj.core.api.Assertions.assertThat(alertsOf(user).get("totalElements").asLong()).isZero();

        mockMvc.perform(json(put("/api/v1/alerts/settings"), user, Map.of("radiusMeters", 1000, "minSeverity", "LOW")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mutedUntil").doesNotExist());
        createReport(reporter, 41.0, 11.0, "LOW");
        org.assertj.core.api.Assertions.assertThat(alertsOf(user).get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void settingsAreValidatedAndHaveDefaults() throws Exception {
        var user = authHelper.registerAndAuthenticateTestUser("alert-settings3@example.com", "password123");

        mockMvc.perform(auth(get("/api/v1/alerts/settings"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radiusMeters").value(1000))
                .andExpect(jsonPath("$.minSeverity").value("LOW"))
                .andExpect(jsonPath("$.latitude").doesNotExist());

        mockMvc.perform(json(put("/api/v1/alerts/settings"), user, Map.of("radiusMeters", 5, "minSeverity", "LOW")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.radiusMeters").exists());
        mockMvc.perform(json(put("/api/v1/alerts/settings/location"), user, Map.of("latitude", 95, "longitude", 0)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.latitude").exists());
    }

    @Test
    void readDeleteAndOwnership() throws Exception {
        var reporter = authHelper.registerAndAuthenticateTestUser("alert-reporter4@example.com", "password123");
        var owner = authHelper.registerAndAuthenticateTestUser("alert-owner4@example.com", "password123");
        var other = authHelper.registerAndAuthenticateTestUser("alert-other4@example.com", "password123");
        sendLocation(owner, 42.0, 12.0);
        sendLocation(other, 42.0, 12.0);
        createReport(reporter, 42.001, 12.0, "MEDIUM");
        createReport(reporter, 42.002, 12.0, "MEDIUM");

        var first = alertsOf(owner).get("content").get(0).get("id").asLong();
        mockMvc.perform(auth(get("/api/v1/alerts/" + first), other)).andExpect(status().isForbidden());
        mockMvc.perform(auth(put("/api/v1/alerts/" + first + "/read"), other)).andExpect(status().isForbidden());
        mockMvc.perform(auth(delete("/api/v1/alerts/" + first), other)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/v1/alerts/999999"), owner)).andExpect(status().isNotFound());

        mockMvc.perform(auth(put("/api/v1/alerts/" + first + "/read"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READ")).andExpect(jsonPath("$.readAt").exists());
        mockMvc.perform(auth(get("/api/v1/alerts?unread=true"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(auth(put("/api/v1/alerts/read-all"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(auth(get("/api/v1/alerts/unread-count"), owner)).andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(auth(delete("/api/v1/alerts/" + first), owner)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/v1/alerts/" + first), owner)).andExpect(status().isNotFound());

        // borrar "todas" solo afecta a quien lo pide
        mockMvc.perform(auth(delete("/api/v1/alerts"), owner)).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(alertsOf(owner).get("totalElements").asLong()).isZero();
        org.assertj.core.api.Assertions.assertThat(alertsOf(other).get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void alertsByUserDistinguishOwnerAdminAndMissingProfile() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("alert-owner5@example.com", "password123");
        var other = authHelper.registerAndAuthenticateTestUser("alert-other5@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("alert-admin5@example.com", "password123");
        var ownerId = profileId(owner, "alert-owner5@example.com");

        mockMvc.perform(auth(get("/api/v1/alerts/user/" + ownerId), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(auth(get("/api/v1/alerts/user/" + ownerId), other)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/v1/alerts/user/" + ownerId), admin)).andExpect(status().isOk());
        mockMvc.perform(auth(get("/api/v1/alerts/user/999999"), admin)).andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/alerts")).andExpect(status().isUnauthorized());
        mockMvc.perform(auth(get("/api/v1/alerts?size=0"), authHelper.registerAndAuthenticateTestUser("alert-size6@example.com", "password123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("size")));
    }
}
