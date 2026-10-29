package com.upc.pre.peaceapp.notification;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Usa su propia zona del mapa (ver {@code AlertsIntegrationTest}). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationsIntegrationTest {

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

    private void createReport(String token, double lat, double lng) throws Exception {
        mockMvc.perform(json(post("/api/v1/reports"), token, Map.of("title", "Incidente", "type", "ACCIDENT",
                "latitude", lat, "longitude", lng, "address", "Av. Prueba 1"))).andExpect(status().isCreated());
    }

    private JsonNode notificationsOf(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(auth(get("/api/v1/notifications"), token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void alertBecomesNotificationThatCanBeReadAndDeleted() throws Exception {
        var reporter = authHelper.registerAndAuthenticateTestUser("notif-reporter1@example.com", "password123");
        var user = authHelper.registerAndAuthenticateTestUser("notif-user1@example.com", "password123");
        sendLocation(user, 50.0, 20.0);
        createReport(reporter, 50.001, 20.0);

        var notifications = notificationsOf(user);
        assertThat(notifications.get("totalElements").asLong()).isEqualTo(1);
        var notification = notifications.get("content").get(0);
        assertThat(notification.get("kind").asText()).isEqualTo("PROXIMITY_ALERT");
        assertThat(notification.get("read").asBoolean()).isFalse();
        var id = notification.get("id").asLong();

        mockMvc.perform(auth(get("/api/v1/notifications/unread-count"), user)).andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(auth(put("/api/v1/notifications/" + id + "/read"), reporter)).andExpect(status().isForbidden());
        mockMvc.perform(auth(put("/api/v1/notifications/" + id + "/read"), user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(auth(get("/api/v1/notifications/unread-count"), user)).andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(auth(delete("/api/v1/notifications/" + id), user)).andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/v1/notifications/" + id), user)).andExpect(status().isNotFound());
    }

    @Test
    void disabledKindOrChannelProducesNoInAppNotification() throws Exception {
        var reporter = authHelper.registerAndAuthenticateTestUser("notif-reporter2@example.com", "password123");
        var user = authHelper.registerAndAuthenticateTestUser("notif-user2@example.com", "password123");
        sendLocation(user, 51.0, 21.0);

        mockMvc.perform(json(put("/api/v1/notifications/preferences"), user,
                        Map.of("inAppEnabled", true, "pushEnabled", true, "disabledKinds", List.of("PROXIMITY_ALERT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disabledKinds[0]").value("PROXIMITY_ALERT"));
        createReport(reporter, 51.001, 21.0);
        assertThat(notificationsOf(user).get("totalElements").asLong()).isZero();

        mockMvc.perform(json(put("/api/v1/notifications/preferences"), user,
                        Map.of("inAppEnabled", false, "pushEnabled", true)))
                .andExpect(status().isOk());
        createReport(reporter, 51.002, 21.0);
        assertThat(notificationsOf(user).get("totalElements").asLong()).isZero();

        // la alerta se genera igualmente: desactivar avisos no oculta el historial de alertas
        mockMvc.perform(auth(get("/api/v1/alerts"), user)).andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(json(put("/api/v1/notifications/preferences"), user, Map.of("inAppEnabled", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devicesCanBeRegisteredReassignedAndRemoved() throws Exception {
        var first = authHelper.registerAndAuthenticateTestUser("notif-device1@example.com", "password123");
        var second = authHelper.registerAndAuthenticateTestUser("notif-device2@example.com", "password123");
        var device = Map.of("token", "fcm-token-abc123", "platform", "ANDROID");

        mockMvc.perform(json(post("/api/v1/notifications/devices"), first, device))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.platform").value("ANDROID"));
        // mismo dispositivo con otra cuenta: no se duplica, cambia de dueño
        mockMvc.perform(json(post("/api/v1/notifications/devices"), second, device)).andExpect(status().isCreated());
        // quien ya no es dueño no puede quitarlo
        mockMvc.perform(auth(delete("/api/v1/notifications/devices?token=fcm-token-abc123"), first))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/v1/notifications/devices?token=fcm-token-abc123"), second))
                .andExpect(status().isNoContent());

        mockMvc.perform(json(post("/api/v1/notifications/devices"), first, Map.of("token", " ", "platform", "IOS")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.token").exists());
    }
}
