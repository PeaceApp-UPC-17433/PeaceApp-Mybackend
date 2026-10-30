package com.upc.pre.peaceapp.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportCommentRepository;
import com.upc.pre.peaceapp.community.infrastructure.persistence.jpa.repositories.ReportVoteRepository;
import com.upc.pre.peaceapp.config.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cada prueba crea sus propios usuarios y reportes porque los contextos de test comparten la base H2.
 * El umbral de verificación es el de por defecto (3 confirmaciones netas).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestAuthHelper authHelper;
    @Autowired private ReportCommentRepository commentRepository;
    @Autowired private ReportVoteRepository voteRepository;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        return builder.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode read(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private long createReport(String token) throws Exception {
        var body = Map.of("title", "Incidente comunidad", "type", "THEFT", "latitude", -33.0, "longitude", -70.0,
                "address", "Av. Prueba 1");
        return read(mockMvc.perform(json(post("/api/v1/reports"), token, body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private long addComment(String token, long reportId, String text) throws Exception {
        return read(mockMvc.perform(json(post("/api/v1/reports/" + reportId + "/comments"), token, Map.of("text", text)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private String reportStatus(String token, long reportId) throws Exception {
        return read(mockMvc.perform(auth(get("/api/v1/reports/" + reportId), token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("status").asText();
    }

    private long flag(String token, String path, String reason) throws Exception {
        return read(mockMvc.perform(json(post(path), token, Map.of("reason", reason)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void commentsAreListedInOrderAndValidated() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("com-owner1@example.com", "password123");
        var other = authHelper.registerAndAuthenticateTestUser("com-other1@example.com", "password123");
        var reportId = createReport(owner);

        addComment(other, reportId, "Yo también lo vi");
        addComment(owner, reportId, "Gracias por confirmar");

        var page = read(mockMvc.perform(auth(get("/api/v1/reports/" + reportId + "/comments"), other))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("content").get(0).get("text").asText()).isEqualTo("Yo también lo vi");

        mockMvc.perform(json(post("/api/v1/reports/" + reportId + "/comments"), other, Map.of("text", "  ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.text").exists());
        mockMvc.perform(json(post("/api/v1/reports/999999/comments"), other, Map.of("text", "hola")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports/" + reportId + "/comments")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAuthorOrAdminDeletesCommentAndAdminDeletionIsLogged() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("com-owner2@example.com", "password123");
        var author = authHelper.registerAndAuthenticateTestUser("com-author2@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("com-admin2@example.com", "password123");
        var reportId = createReport(owner);
        var ownComment = addComment(author, reportId, "Mi comentario");
        var moderated = addComment(author, reportId, "Comentario a moderar");

        mockMvc.perform(auth(delete("/api/v1/comments/" + ownComment), owner)).andExpect(status().isForbidden());
        mockMvc.perform(auth(delete("/api/v1/comments/" + ownComment), author)).andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/v1/comments/" + ownComment), author)).andExpect(status().isNotFound());

        mockMvc.perform(auth(delete("/api/v1/comments/" + moderated), admin)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/v1/reports/" + reportId + "/comments"), owner))
                .andExpect(jsonPath("$.totalElements").value(0));

        var log = read(mockMvc.perform(auth(get("/api/v1/admin/moderation-log?size=100"), admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(log.get("content")).anySatisfy(entry -> {
            assertThat(entry.get("type").asText()).isEqualTo("COMMENT_REMOVED");
            assertThat(entry.get("targetId").asLong()).isEqualTo(moderated);
        });
        // Retirar el propio comentario no es una acción de moderación.
        assertThat(log.get("content")).noneSatisfy(entry -> assertThat(entry.get("targetId").asLong()).isEqualTo(ownComment));
    }

    @Test
    void votesVerifyTheReportWhenThresholdIsReached() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("vote-owner3@example.com", "password123");
        var voters = new String[3];
        for (int i = 0; i < voters.length; i++)
            voters[i] = authHelper.registerAndAuthenticateTestUser("vote-user3" + i + "@example.com", "password123");
        var reportId = createReport(owner);
        var votePath = "/api/v1/reports/" + reportId + "/vote";

        mockMvc.perform(json(put(votePath), owner, Map.of("type", "CONFIRM"))).andExpect(status().isForbidden());
        mockMvc.perform(json(put(votePath), voters[0], Map.of("type", "BOGUS"))).andExpect(status().isBadRequest());

        mockMvc.perform(json(put(votePath), voters[0], Map.of("type", "CONFIRM")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.confirmations").value(1))
                .andExpect(jsonPath("$.myVote").value("CONFIRM")).andExpect(jsonPath("$.verificationThreshold").value(3));
        mockMvc.perform(json(put(votePath), voters[1], Map.of("type", "CONFIRM"))).andExpect(status().isOk());
        assertThat(reportStatus(owner, reportId)).isEqualTo("PENDING");

        mockMvc.perform(json(put(votePath), voters[2], Map.of("type", "CONFIRM")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.net").value(3));
        assertThat(reportStatus(owner, reportId)).isEqualTo("VERIFIED");

        // Votar de nuevo no duplica el voto: lo reemplaza.
        mockMvc.perform(json(put(votePath), voters[2], Map.of("type", "CONFIRM")))
                .andExpect(jsonPath("$.confirmations").value(3));
        assertThat(voteRepository.countByReportIdAndType(reportId, com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType.CONFIRM)).isEqualTo(3);
    }

    @Test
    void voteCanChangeBeRetractedAndOtherUsersSeeTheirOwnStance() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("vote-owner4@example.com", "password123");
        var voter = authHelper.registerAndAuthenticateTestUser("vote-user4@example.com", "password123");
        var reportId = createReport(owner);
        var votePath = "/api/v1/reports/" + reportId + "/vote";

        mockMvc.perform(json(put(votePath), voter, Map.of("type", "CONFIRM"))).andExpect(status().isOk());
        mockMvc.perform(json(put(votePath), voter, Map.of("type", "DISPUTE")))
                .andExpect(jsonPath("$.confirmations").value(0)).andExpect(jsonPath("$.disputes").value(1))
                .andExpect(jsonPath("$.net").value(-1)).andExpect(jsonPath("$.myVote").value("DISPUTE"));

        mockMvc.perform(auth(get("/api/v1/reports/" + reportId + "/votes"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disputes").value(1))
                .andExpect(jsonPath("$.myVote").doesNotExist());

        mockMvc.perform(auth(delete(votePath), voter)).andExpect(status().isOk())
                .andExpect(jsonPath("$.disputes").value(0)).andExpect(jsonPath("$.myVote").doesNotExist());
        mockMvc.perform(auth(delete(votePath), voter)).andExpect(status().isOk()); // idempotente
        mockMvc.perform(auth(get("/api/v1/reports/999999/votes"), voter)).andExpect(status().isNotFound());
    }

    @Test
    void closedReportsNoLongerAcceptVotes() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("vote-owner5@example.com", "password123");
        var voter = authHelper.registerAndAuthenticateTestUser("vote-user5@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("vote-admin5@example.com", "password123");
        var reportId = createReport(owner);
        mockMvc.perform(json(put("/api/v1/reports/" + reportId + "/status"), admin, Map.of("status", "DISCARDED")))
                .andExpect(status().isOk());

        mockMvc.perform(json(put("/api/v1/reports/" + reportId + "/vote"), voter, Map.of("type", "CONFIRM")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("El reporte ya no admite votos"));
    }

    @Test
    void flagsAreValidatedAndDeduplicated() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("flag-owner6@example.com", "password123");
        var reporter = authHelper.registerAndAuthenticateTestUser("flag-user6@example.com", "password123");
        var reportId = createReport(owner);
        var commentId = addComment(owner, reportId, "Comentario denunciable");

        mockMvc.perform(json(post("/api/v1/reports/" + reportId + "/flag"), owner, Map.of("reason", "SPAM")))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/v1/reports/" + reportId + "/flag"), reporter, Map.of("details", "sin motivo")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.reason").exists());
        mockMvc.perform(json(post("/api/v1/reports/999999/flag"), reporter, Map.of("reason", "SPAM")))
                .andExpect(status().isNotFound());
        mockMvc.perform(json(post("/api/v1/comments/999999/flag"), reporter, Map.of("reason", "SPAM")))
                .andExpect(status().isNotFound());

        flag(reporter, "/api/v1/reports/" + reportId + "/flag", "FALSE_INFORMATION");
        mockMvc.perform(json(post("/api/v1/reports/" + reportId + "/flag"), reporter, Map.of("reason", "SPAM")))
                .andExpect(status().isConflict());
        flag(reporter, "/api/v1/comments/" + commentId + "/flag", "OFFENSIVE");
    }

    @Test
    void adminPanelRejectsRegularUsers() throws Exception {
        var user = authHelper.registerAndAuthenticateTestUser("flag-user7@example.com", "password123");
        mockMvc.perform(auth(get("/api/v1/admin/flags"), user)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(auth(get("/api/v1/admin/moderation-log"), user)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/v1/admin/metrics"), user)).andExpect(status().isForbidden());
        mockMvc.perform(json(put("/api/v1/admin/flags/1/resolve"), user, Map.of("resolution", "DISMISSED")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/flags")).andExpect(status().isUnauthorized());
    }

    @Test
    void takingActionOnACommentRemovesItAndClosesTheOtherFlags() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("flag-owner8@example.com", "password123");
        var first = authHelper.registerAndAuthenticateTestUser("flag-user8a@example.com", "password123");
        var second = authHelper.registerAndAuthenticateTestUser("flag-user8b@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("flag-admin8@example.com", "password123");
        var reportId = createReport(owner);
        var commentId = addComment(owner, reportId, "Comentario ofensivo");
        var firstFlag = flag(first, "/api/v1/comments/" + commentId + "/flag", "OFFENSIVE");
        var secondFlag = flag(second, "/api/v1/comments/" + commentId + "/flag", "SPAM");

        var open = read(mockMvc.perform(auth(get("/api/v1/admin/flags?status=OPEN&size=100"), admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(open.get("content")).extracting(n -> n.get("id").asLong()).contains(firstFlag, secondFlag);

        mockMvc.perform(json(put("/api/v1/admin/flags/" + firstFlag + "/resolve"), admin,
                        Map.of("resolution", "ACTION_TAKEN", "note", "Lenguaje ofensivo")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTION_TAKEN"))
                .andExpect(jsonPath("$.resolutionNote").value("Lenguaje ofensivo"));

        mockMvc.perform(auth(get("/api/v1/reports/" + reportId + "/comments"), owner))
                .andExpect(jsonPath("$.totalElements").value(0));
        var all = read(mockMvc.perform(auth(get("/api/v1/admin/flags?status=ACTION_TAKEN&size=100"), admin))
                .andReturn().getResponse().getContentAsString());
        assertThat(all.get("content")).extracting(n -> n.get("id").asLong()).contains(firstFlag, secondFlag);

        mockMvc.perform(json(put("/api/v1/admin/flags/" + secondFlag + "/resolve"), admin, Map.of("resolution", "DISMISSED")))
                .andExpect(status().isConflict());
        mockMvc.perform(json(put("/api/v1/admin/flags/" + firstFlag + "/resolve"), admin, Map.of("resolution", "OPEN")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(put("/api/v1/admin/flags/999999/resolve"), admin, Map.of("resolution", "DISMISSED")))
                .andExpect(status().isNotFound());
    }

    @Test
    void takingActionOnAReportDiscardsItAndDismissingLeavesItUntouched() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("flag-owner9@example.com", "password123");
        var reporter = authHelper.registerAndAuthenticateTestUser("flag-user9@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("flag-admin9@example.com", "password123");
        var abusive = createReport(owner);
        var fine = createReport(owner);
        var abusiveFlag = flag(reporter, "/api/v1/reports/" + abusive + "/flag", "FALSE_INFORMATION");
        var fineFlag = flag(reporter, "/api/v1/reports/" + fine + "/flag", "DUPLICATE");

        mockMvc.perform(json(put("/api/v1/admin/flags/" + abusiveFlag + "/resolve"), admin, Map.of("resolution", "ACTION_TAKEN")))
                .andExpect(status().isOk());
        mockMvc.perform(json(put("/api/v1/admin/flags/" + fineFlag + "/resolve"), admin, Map.of("resolution", "DISMISSED")))
                .andExpect(status().isOk());

        assertThat(reportStatus(owner, abusive)).isEqualTo("DISCARDED");
        assertThat(reportStatus(owner, fine)).isEqualTo("PENDING");

        var log = read(mockMvc.perform(auth(get("/api/v1/admin/moderation-log?size=100"), admin))
                .andReturn().getResponse().getContentAsString());
        assertThat(log.get("content")).extracting(n -> n.get("type").asText() + ":" + n.get("targetId").asLong())
                .contains("REPORT_DISCARDED:" + abusive, "FLAG_DISMISSED:" + fine);
    }

    @Test
    void deletingAReportRemovesItsCommunityData() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("del-owner10@example.com", "password123");
        var user = authHelper.registerAndAuthenticateTestUser("del-user10@example.com", "password123");
        var reportId = createReport(owner);
        var commentId = addComment(user, reportId, "Se irá con el reporte");
        mockMvc.perform(json(put("/api/v1/reports/" + reportId + "/vote"), user, Map.of("type", "CONFIRM"))).andExpect(status().isOk());
        flag(user, "/api/v1/reports/" + reportId + "/flag", "SPAM");
        flag(owner, "/api/v1/comments/" + commentId + "/flag", "SPAM");
        assertThat(commentRepository.findIdsByReportId(reportId)).hasSize(1);

        mockMvc.perform(auth(delete("/api/v1/reports/" + reportId), owner)).andExpect(status().isNoContent());

        assertThat(commentRepository.findIdsByReportId(reportId)).isEmpty();
        assertThat(voteRepository.countByReportIdAndType(reportId, com.upc.pre.peaceapp.community.domain.model.valueobjects.VoteType.CONFIRM)).isZero();
    }

    @Test
    void metricsSummarizeReportsAndCommunityActivity() throws Exception {
        var owner = authHelper.registerAndAuthenticateTestUser("met-owner11@example.com", "password123");
        var user = authHelper.registerAndAuthenticateTestUser("met-user11@example.com", "password123");
        var admin = authHelper.registerAndAuthenticateTestAdmin("met-admin11@example.com", "password123");
        var reportId = createReport(owner);
        addComment(user, reportId, "Comentario para métricas");
        mockMvc.perform(json(put("/api/v1/reports/" + reportId + "/vote"), user, Map.of("type", "CONFIRM"))).andExpect(status().isOk());
        flag(user, "/api/v1/reports/" + reportId + "/flag", "SPAM");
        mockMvc.perform(json(put("/api/v1/reports/" + reportId + "/status"), admin, Map.of("status", "VERIFIED"))).andExpect(status().isOk());

        var metrics = read(mockMvc.perform(auth(get("/api/v1/admin/metrics"), admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(metrics.get("reports").get("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.get("reports").get("byStatus").get("VERIFIED").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.get("reports").get("verified").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.get("reports").get("verifiedRate").asDouble()).isBetween(0.0, 1.0);
        assertThat(metrics.get("reports").get("averageAttentionHours").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(metrics.get("community").get("comments").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.get("community").get("confirmations").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(metrics.get("flags").get("open").asLong()).isGreaterThanOrEqualTo(1);
    }
}
