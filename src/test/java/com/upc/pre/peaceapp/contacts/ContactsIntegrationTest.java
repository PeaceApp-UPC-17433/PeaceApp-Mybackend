package com.upc.pre.peaceapp.contacts;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cada prueba crea sus propios usuarios porque los contextos de test comparten la base H2. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactsIntegrationTest {
    private static final String PASSWORD = "password123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;

    private record Person(String email, String token) {
    }

    private Person person() throws Exception {
        var email = "contact-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        return new Person(email, authHelper.registerAndAuthenticateTestUser(email, PASSWORD));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, Person who) {
        return builder.header("Authorization", "Bearer " + who.token());
    }

    private JsonNode read(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private void sendRequest(Person from, Person to) throws Exception {
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), from).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", to.email()))))
                .andExpect(status().isCreated());
    }

    private long incomingId(Person who) throws Exception {
        var list = read(mockMvc.perform(auth(get("/api/v1/contacts/requests/incoming"), who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(list).hasSize(1);
        return list.get(0).get("id").asLong();
    }

    private JsonNode list(String path, Person who) throws Exception {
        return read(mockMvc.perform(auth(get(path), who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void requestAcceptAndListAsContacts() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);

        var outgoing = list("/api/v1/contacts/requests/outgoing", ana);
        assertThat(outgoing).hasSize(1);
        assertThat(outgoing.get(0).get("email").asText()).isEqualTo(luis.email());
        assertThat(outgoing.get(0).get("status").asText()).isEqualTo("PENDING");
        assertThat(outgoing.get(0).get("initiatedByMe").asBoolean()).isTrue();

        var id = incomingId(luis);
        assertThat(list("/api/v1/contacts", luis)).isEmpty();
        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), luis)).andExpect(status().isNoContent());

        for (var who : new Person[]{ana, luis}) {
            var contacts = list("/api/v1/contacts", who);
            assertThat(contacts).hasSize(1);
            assertThat(contacts.get(0).get("status").asText()).isEqualTo("ACCEPTED");
        }
        assertThat(list("/api/v1/contacts/requests/outgoing", ana)).isEmpty();
    }

    @Test
    void requestAndAcceptanceNotifyTheOtherPerson() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);
        var luisNotifications = list("/api/v1/notifications", luis).get("content");
        assertThat(luisNotifications).hasSize(1);
        assertThat(luisNotifications.get(0).get("kind").asText()).isEqualTo("CONTACT_REQUEST");

        mockMvc.perform(auth(put("/api/v1/contacts/" + incomingId(luis) + "/accept"), luis))
                .andExpect(status().isNoContent());
        var anaNotifications = list("/api/v1/notifications", ana).get("content");
        assertThat(anaNotifications).hasSize(1);
        assertThat(anaNotifications.get(0).get("kind").asText()).isEqualTo("CONTACT_ACCEPTED");
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        var ana = person();
        var luis = person();
        var body = objectMapper.writeValueAsString(Map.of("email", ana.email()));
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), ana).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-es-correo\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com\"}")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/contacts/requests").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isUnauthorized());

        sendRequest(ana, luis);
        // Repetida, o cruzada en sentido contrario: 409
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), ana).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", luis.email())))).andExpect(status().isConflict());
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), luis).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isConflict());
    }

    @Test
    void onlyTheAddresseeCanRespondAndOutsidersGetNotFound() throws Exception {
        var ana = person();
        var luis = person();
        var outsider = person();
        sendRequest(ana, luis);
        var id = incomingId(luis);

        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), ana)).andExpect(status().isForbidden());
        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), outsider)).andExpect(status().isNotFound());
        mockMvc.perform(auth(delete("/api/v1/contacts/" + id), outsider)).andExpect(status().isNotFound());
        mockMvc.perform(auth(put("/api/v1/contacts/999999/accept"), luis)).andExpect(status().isNotFound());

        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), luis)).andExpect(status().isNoContent());
        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), luis)).andExpect(status().isConflict());
    }

    @Test
    void declineDeletesTheRequestSoItCanBeSentAgain() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);
        mockMvc.perform(auth(put("/api/v1/contacts/" + incomingId(luis) + "/decline"), luis))
                .andExpect(status().isNoContent());
        assertThat(list("/api/v1/contacts/requests/incoming", luis)).isEmpty();
        sendRequest(ana, luis);
    }

    @Test
    void removingAContactWorksForEitherSide() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);
        var id = incomingId(luis);
        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/accept"), luis)).andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/v1/contacts/" + id), ana)).andExpect(status().isNoContent());
        assertThat(list("/api/v1/contacts", luis)).isEmpty();
    }

    @Test
    void blockingHidesTheRelationAndOnlyTheBlockerCanUndoIt() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);
        var id = incomingId(luis);
        mockMvc.perform(auth(put("/api/v1/contacts/" + id + "/block"), luis)).andExpect(status().isNoContent());

        assertThat(list("/api/v1/contacts/blocked", luis)).hasSize(1);
        assertThat(list("/api/v1/contacts/blocked", ana)).isEmpty();
        assertThat(list("/api/v1/contacts/requests/outgoing", ana)).isEmpty();

        // Ana ya no puede reenviar ni quitar el bloqueo de Luis
        mockMvc.perform(auth(post("/api/v1/contacts/requests"), ana).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", luis.email())))).andExpect(status().isConflict());
        mockMvc.perform(auth(delete("/api/v1/contacts/" + id), ana)).andExpect(status().isForbidden());

        mockMvc.perform(auth(delete("/api/v1/contacts/" + id), luis)).andExpect(status().isNoContent());
        sendRequest(ana, luis);
    }

    @Test
    void deletingAProfileRemovesItsContacts() throws Exception {
        var ana = person();
        var luis = person();
        sendRequest(ana, luis);
        mockMvc.perform(auth(put("/api/v1/contacts/" + incomingId(luis) + "/accept"), luis))
                .andExpect(status().isNoContent());
        var profileId = read(mockMvc.perform(auth(get("/api/v1/users/" + ana.email()), ana)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(auth(delete("/api/v1/users/" + profileId), ana)).andExpect(status().isNoContent());
        assertThat(list("/api/v1/contacts", luis)).isEmpty();
    }
}
